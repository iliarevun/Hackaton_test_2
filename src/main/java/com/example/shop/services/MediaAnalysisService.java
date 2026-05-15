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

/**
 * Module 1 — Media Analysis
 * Analyzes uploaded text for AI bias, fake news, and propaganda using Gemini.
 */
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

    public Map<String, Object> analyzeMedia(String text) {
        String safeText = text.length() > 3000 ? text.substring(0, 3000) : text;

        // Build content parts with text in a separate part so user text never
        // interferes with the instruction.
        String instruction = """
            Ти — експерт з медіаграмотності та критичного мислення. Проаналізуй наданий текст нижче.
            Визнач наявність:
            1. Упередженості ШІ — чи текст написаний або оброблений ШІ з характерними патернами
            2. Фейку — чи є ознаки дезінформації, маніпуляції фактами, відсутності джерел
            3. Пропаганди — чи використовуються маніпулятивні техніки (емоційна мова, чорно-біле мислення, апеляція до страху/ненависті)

            ТЕКСТ ДЛЯ АНАЛІЗУ:
            ---
            """ + safeText + """
            ---

            Поверни ВИКЛЮЧНО валідний JSON без жодного markdown, без коментарів, без тексту до або після:
            {
              "overallRisk": "LOW",
              "overallScore": 12,
              "aiBias": { "detected": false, "score": 10, "explanation": "..." },
              "fakeNews": { "detected": false, "score": 15, "explanation": "..." },
              "propaganda": { "detected": false, "score": 8, "explanation": "...", "techniques": [] },
              "summary": "...",
              "recommendation": "..."
            }
            """;

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instruction)))),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 8192
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
            log.debug("Gemini raw response: {}", response.getBody().substring(0, Math.min(500, response.getBody().length())));

            // Collect ALL text parts (Gemini 2.5 may return thinking + answer parts)
            String raw = "";
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");
            if (parts.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    String t = part.path("text").asText("");
                    if (!t.isBlank()) sb.append(t);
                }
                raw = sb.toString();
            }

            // Strip markdown fences and extract first JSON object
            raw = raw.replaceAll("(?s)```json\s*", "").replaceAll("(?s)```\s*", "").trim();
            // Find the outermost JSON object
            int start = raw.indexOf("{");
            int end   = raw.lastIndexOf("}");
            if (start >= 0 && end > start) raw = raw.substring(start, end + 1);

            JsonNode result = objectMapper.readTree(raw);
            return objectMapper.convertValue(result, Map.class);

        } catch (Exception e) {
            log.error("Media analysis error: {}", e.getMessage());
            return Map.of(
                    "overallRisk", "UNKNOWN",
                    "overallScore", 0,
                    "aiBias",     Map.of("detected", false, "score", 0, "explanation", "Помилка аналізу"),
                    "fakeNews",   Map.of("detected", false, "score", 0, "explanation", "Помилка аналізу"),
                    "propaganda", Map.of("detected", false, "score", 0, "explanation", "Помилка аналізу", "techniques", List.of()),
                    "summary",        "Не вдалося проаналізувати текст: " + e.getMessage(),
                    "recommendation", "Спробуйте ще раз або скоротіть текст."
            );
        }
    }
}
