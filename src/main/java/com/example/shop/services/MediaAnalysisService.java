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
        // Trim text and sanitize: escape backslashes and double-quotes so the
        // text can safely sit inside a JSON string in the prompt.
        String safeText = (text.length() > 3000 ? text.substring(0, 3000) : text)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        // Use a structured prompt with the text as a proper JSON field
        // so user input cannot break the outer JSON structure.
        String prompt = """
            Ти — експерт з медіаграмотності та критичного мислення. Проаналізуй наданий текст.
            Визнач наявність:
            1. **Упередженості ШІ** — чи текст написаний або оброблений ШІ з характерними патернами
            2. **Фейку** — чи є ознаки дезінформації, маніпуляції фактами, відсутності джерел
            3. **Пропаганди** — чи використовуються маніпулятивні техніки (емоційна мова, чорно-біле мислення, апеляція до страху/ненависті)

            Текст для аналізу:
            "%s"

            Відповідь ТІЛЬКИ у форматі JSON (без markdown, без пояснень поза JSON):
            {
              "overallRisk": "LOW|MEDIUM|HIGH",
              "overallScore": 0-100,
              "aiBias": {
                "detected": true/false,
                "score": 0-100,
                "explanation": "коротке пояснення"
              },
              "fakeNews": {
                "detected": true/false,
                "score": 0-100,
                "explanation": "коротке пояснення"
              },
              "propaganda": {
                "detected": true/false,
                "score": 0-100,
                "explanation": "коротке пояснення",
                "techniques": ["техніка1", "техніка2"]
              },
              "summary": "загальний висновок 2-3 речення",
              "recommendation": "що робити читачу"
            }
            """.formatted(safeText);

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 1500)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String raw = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Strip markdown fences if present
            raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            JsonNode result = objectMapper.readTree(raw);
            return objectMapper.convertValue(result, Map.class);

        } catch (Exception e) {
            log.error("Media analysis error: {}", e.getMessage());
            return Map.of(
                    "overallRisk", "UNKNOWN",
                    "overallScore", 0,
                    "summary", "Не вдалося проаналізувати текст. Спробуйте ще раз.",
                    "error", e.getMessage()
            );
        }
    }
}
