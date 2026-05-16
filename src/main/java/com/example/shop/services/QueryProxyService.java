package com.example.shop.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module 2 — Query Proxy
 * Strips PII / sensitive data from user queries before forwarding to AI,
 * then restores the original context in the returned answer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryProxyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // Regex patterns for PII detection
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d[\\s\\-]?){9,14}\\d");
    private static final Pattern PASSPORT_PATTERN =
            Pattern.compile("[A-ZА-Я]{2}\\d{6}");
    private static final Pattern CARD_PATTERN =
            Pattern.compile("\\b\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b");
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern IBAN_PATTERN =
            Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{1,30}\\b");

    public record SanitizedQuery(String sanitized, Map<String, String> replacements) {}

    /**
     * Sanitize a raw user query: replace PII tokens with placeholders.
     */
    public SanitizedQuery sanitize(String raw) {
        Map<String, String> replacements = new java.util.LinkedHashMap<>();
        String sanitized = raw;
        int counter = 1;

        sanitized = replaceAll(sanitized, EMAIL_PATTERN,   "EMAIL_",   replacements, counter);
        counter += replacements.size();
        sanitized = replaceAll(sanitized, PHONE_PATTERN,   "PHONE_",   replacements, counter);
        counter += replacements.size();
        sanitized = replaceAll(sanitized, CARD_PATTERN,    "CARD_",    replacements, counter);
        counter += replacements.size();
        sanitized = replaceAll(sanitized, IBAN_PATTERN,    "IBAN_",    replacements, counter);
        counter += replacements.size();
        sanitized = replaceAll(sanitized, PASSPORT_PATTERN,"PASS_",    replacements, counter);
        counter += replacements.size();
        sanitized = replaceAll(sanitized, IP_PATTERN,      "IP_",      replacements, counter);

        log.debug("Sanitized query: {} replacements applied", replacements.size());
        return new SanitizedQuery(sanitized, replacements);
    }

    private String replaceAll(String text, Pattern pattern, String prefix,
                              Map<String, String> map, int startIdx) {
        Matcher m = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        int i = startIdx;
        while (m.find()) {
            String original = m.group();
            String token = "[" + prefix + i++ + "]";
            if (!map.containsValue(original)) {
                map.put(token, original);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(token));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Send the sanitized query to Gemini and return the AI response.
     */
    public String askAI(String sanitizedQuery, String systemContext) {
        String fullPrompt = systemContext != null && !systemContext.isBlank()
                ? systemContext + "\n\nUser query: " + sanitizedQuery
                : sanitizedQuery;

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt)))),
                    "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 2000)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.error("QueryProxy AI error: {}", e.getMessage());
            return "Failed to get a response from the AI. Please try again.";
        }
    }

    /**
     * Full pipeline: sanitize → ask AI → restore placeholders in response.
     */
    public Map<String, Object> proxyQuery(String rawQuery, String systemContext) {
        SanitizedQuery sq = sanitize(rawQuery);
        String aiResponse = askAI(sq.sanitized(), systemContext);

        // Restore original values in the AI response
        String restored = aiResponse;
        for (Map.Entry<String, String> entry : sq.replacements().entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }

        return Map.of(
                "originalQuery",   rawQuery,
                "sanitizedQuery",  sq.sanitized(),
                "replacementsCount", sq.replacements().size(),
                "detectedPII",     sq.replacements().values().stream().toList(),
                "aiResponse",      restored,
                "privacySafe",     true
        );
    }
}
