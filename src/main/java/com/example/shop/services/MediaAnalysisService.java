package com.example.shop.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // Cache: SHA-256(text) → result. Same text always returns the same scores.
    private static final Map<String, Map<String, Object>> CACHE = new ConcurrentHashMap<>();

    public Map<String, Object> analyzeMedia(String text) {
        String safeText = text.strip();
        if (safeText.length() > 3000) safeText = safeText.substring(0, 3000);

        // Cache lookup — identical text → identical result, no randomness
        String cacheKey = sha256(safeText);
        Map<String, Object> cached = CACHE.get(cacheKey);
        if (cached != null) {
            log.info("Media analysis cache hit ({}...)", cacheKey.substring(0, 8));
            return cached;
        }

        /*
         * Deterministic scoring rules — the model acts as a calculator, not a storyteller.
         * Google Search is enabled so the model can verify facts against live sources,
         * but the SCORING FORMULA is fixed, so live results only change the explanation
         * text, not the numerical scores arbitrarily.
         *
         * Score rules (applied mechanically):
         *   aiBias   +10 per AI-writing signal: uniform sentence length, no personal voice,
         *             generic transitions, "it is important to note", passive overuse.
         *   fakeNews +15 per issue: unverifiable claim, contradicts Google Search result,
         *             missing date/source, sensational wording with no evidence.
         *   propaganda +12 per technique: us-vs-them, appeal to fear, bandwagon,
         *             cherry-picking, repetition of slogans, glittering generalities.
         *   overallScore = round((aiBias + fakeNews + propaganda) / 3)
         *   overallRisk  = LOW if overallScore < 30, MEDIUM if 30-59, HIGH if >= 60
         *   detected     = true if that category score >= 40
         */
        String instruction = """
            You are a deterministic media-analysis engine with Google Search access.
            Use Google Search ONLY to verify factual claims in the text against live sources.
            Do NOT use search results to change your scoring formula — the formula below is fixed.

            SCORING FORMULA (apply mechanically, do not deviate):
            • aiBias.score   (0-100): start at 0, add 10 for EACH of these signals found:
                uniform sentence length throughout, no personal voice or anecdotes,
                generic transitions ("furthermore","it is important to note"),
                excessive passive voice, lack of specific named sources/dates.
            • fakeNews.score (0-100): start at 0, add 15 for EACH issue found:
                claim contradicted by Google Search results, missing date or source,
                unverifiable statistic, sensational headline with no evidence.
            • propaganda.score (0-100): start at 0, add 12 for EACH technique present:
                us-vs-them framing, appeal to fear, bandwagon, cherry-picking,
                repetition of slogans, glittering generalities, appeal to authority.
            • overallScore = integer average of the three scores above (round down).
            • overallRisk  = "LOW" if overallScore < 30 | "MEDIUM" if 30-59 | "HIGH" if >= 60.
            • detected     = true if that category's score >= 40, else false.

            TEXT TO ANALYZE:
            ---
            """ + safeText + """
            ---

            Return ONLY raw JSON — no markdown fences, no preamble:
            {
              "overallRisk": "LOW",
              "overallScore": 0,
              "aiBias":    { "detected": false, "score": 0, "explanation": "..." },
              "fakeNews":  { "detected": false, "score": 0, "explanation": "..." },
              "propaganda":{ "detected": false, "score": 0, "explanation": "...", "techniques": [] },
              "summary": "...",
              "recommendation": "..."
            }
            """;

        try {
            // temperature=0 → deterministic token selection from the model
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instruction)))),
                    "tools", List.of(Map.of("googleSearch", Map.of())),
                    "generationConfig", Map.of(
                            "temperature", 0,
                            "topP", 1,
                            "topK", 1
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            // Collect all text parts (googleSearch may produce multiple parts)
            String raw = "";
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            if (parts.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    String t = part.path("text").asText("");
                    if (!t.isBlank()) sb.append(t);
                }
                raw = sb.toString().trim();
            }

            // Strip accidental markdown fences
            if (raw.contains("```")) {
                raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            }

            // Extract the JSON object
            int start = raw.indexOf("{");
            int end   = raw.lastIndexOf("}");
            if (start >= 0 && end > start) {
                raw = raw.substring(start, end + 1);
            }

            JsonNode result = objectMapper.readTree(raw);
            Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);

            // Cache so repeated calls for the same text return identical results
            CACHE.put(cacheKey, resultMap);
            log.info("Media analysis complete and cached ({}...)", cacheKey.substring(0, 8));
            return resultMap;

        } catch (Exception e) {
            log.error("Media analysis failure: {}", e.getMessage());
            return Map.of(
                    "overallRisk",  "UNKNOWN",
                    "overallScore", 0,
                    "aiBias",     Map.of("detected", false, "score", 0, "explanation", "Analysis failed."),
                    "fakeNews",   Map.of("detected", false, "score", 0, "explanation", "Analysis failed."),
                    "propaganda", Map.of("detected", false, "score", 0, "explanation", e.getMessage(), "techniques", List.of()),
                    "summary",        "An error occurred during analysis.",
                    "recommendation", "Check your API quota or try again later."
            );
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
