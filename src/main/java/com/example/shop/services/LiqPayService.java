package com.example.shop.services;

import com.example.shop.models.LiqPayResponse;
import com.example.shop.models.User;
import lombok.AllArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Base64;

import java.util.Locale;
import java.util.Map;

@Service
@AllArgsConstructor
public class LiqPayService {

    // Ваша конфігурація (PRIVATE_KEY і PUBLIC_KEY)
    private static final String PRIVATE_KEY = "sandbox_CiZtVBZlxopZFMrt42Sd3TvM3bK6xE0wanyq35h4";
    private static final String PUBLIC_KEY = "sandbox_i98216480294";

    public String generateData(int amount, String currency, String description,
                               String resultURL, String callBackURL) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", "3");
        json.put("action", "pay");
        json.put("amount", amount);
        json.put("currency", currency);
        json.put("description", description);
        json.put("result_url", resultURL);
        json.put("callback_url", callBackURL);
        json.put("public_key", PUBLIC_KEY);



        return Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));

    }

    public String generateSignature(String data) {
        try {
            String signatureString = PRIVATE_KEY + data + PRIVATE_KEY;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(signatureString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Не вдалося створити підпис", e);
        }
    }

    public LiqPayResponse parseCallback(Map<String, String> requestParams) throws JSONException {
        System.out.println("Request body: " + requestParams);
        String data = requestParams.get("data");
        String signature = requestParams.get("signature");

        if (data == null || signature == null) {
            throw new IllegalArgumentException("Некоректні параметри callback.");
        }

        // Перевірка підпису
        String generatedSignature = generateSignature(data);
        if (!generatedSignature.equals(signature)) {
            throw new SecurityException("Підпис callback не відповідає.");
        }

        // Розшифровка даних
        String decodedData = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(decodedData);

        System.out.println("JSON received: " + json);

        // Витягнення даних для User
        User user = new User();
        user.setId(json.optLong("user_id"));
        user.setName(json.optString("user_name", "Unknown"));
        user.setEmail(json.optString("user_email", "Unknown"));

        // Витягнення інших даних
        String os = json.optString("os", "unknown");
        String status = json.optString("status");

        // Перевірка статусу успішності
        boolean isSuccess = "success".equalsIgnoreCase(status);

        return new LiqPayResponse(user, os, isSuccess);
    }
}