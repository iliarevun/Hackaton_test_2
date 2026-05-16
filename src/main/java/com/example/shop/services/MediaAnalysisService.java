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

        String instruction = """
            Ти — експерт з медіаграмотності, фактчекінгу та критичного мислення. 
            Тобі надано доступ до інструменту пошуку Google Search. Використовуй його, щоб знайти найсвіжішу та найактуальнішу інформацію про події, згадані в тексті, особливо якщо вони стосуються сьогодення.
            
            Проаналізуй наданий текст і порівняй його з реальними фактами з пошукової видачі.
            Визнач наявність:
            1. Упередженості ШІ — чи текст згенерований штучним інтелектом.
            2. Фейку — чи суперечить текст офіційним даним, свіжим новинам, чи є це дезінформацією або маніпуляцією.
            3. Пропаганди — чи використовуються маніпулятивні техніки.

            ТЕКСТ ДЛЯ АНАЛІЗУ:
            ---
            """ + safeText + """
            ---

            Поверни відповідь СУВОРO у форматі JSON за такою схемою (без markdown ` ```json `):
            {
              "overallRisk": "LOW",
              "overallScore": 12,
              "aiBias": { "detected": false, "score": 10, "explanation": "Опис українською мовою..." },
              "fakeNews": { "detected": false, "score": 15, "explanation": "Опис на основі свіжих знахідок з Google Search українською мовою..." },
              "propaganda": { "detected": false, "score": 8, "explanation": "Опис українською мовою...", "techniques": ["Назва техніки"] },
              "summary": "Загальний підсумок аналізу з урахуванням новин за сьогодні...",
              "recommendation": "Рекомендація для користувача українською..."
            }
            """;

        try {
            // Формуємо запит: прибираємо responseMimeType, залишаємо тільки googleSearch
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instruction)))),
                    "tools", List.of(Map.of("googleSearch", Map.of())),
                    "generationConfig", Map.of(
                            "temperature", 0.1
                            // ПРИБРАНО конфліктний "responseMimeType": "application/json"
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

            // Збираємо текст відповіді
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

            // Наше очищення тепер самостійно впорається з маркдауном, який поверне модель
            raw = raw.trim();
            if (raw.contains("```")) {
                raw = raw.replaceAll("(?s)```json\s*", "").replaceAll("(?s)```\s*", "").trim();
            }

            int start = raw.indexOf("{");
            int end   = raw.lastIndexOf("}");
            if (start >= 0 && end > start) {
                raw = raw.substring(start, end + 1);
            }

            JsonNode result = objectMapper.readTree(raw);
            return objectMapper.convertValue(result, Map.class);

        } catch (Exception e) {
            log.error("Media analysis search grounding failure: {}", e.getMessage());
            return Map.of(
                    "overallRisk", "UNKNOWN",
                    "overallScore", 0,
                    "aiBias",     Map.of("detected", false, "score", 0, "explanation", "Помилка мережі при пошуку фактів."),
                    "fakeNews",   Map.of("detected", false, "score", 0, "explanation", "Не вдалося виконати онлайн-перевірку фактів."),
                    "propaganda", Map.of("detected", false, "score", 0, "explanation", e.getMessage(), "techniques", List.of()),
                    "summary",        "Помилка інтеграції з Google Search Grounding.",
                    "recommendation", "Перевірте баланс лімітів API або повторіть запит пізніше."
            );
        }
    }
}