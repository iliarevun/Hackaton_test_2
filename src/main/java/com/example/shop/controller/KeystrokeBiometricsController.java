package com.example.shop.controller;

import com.example.shop.models.User;
import com.example.shop.services.KeystrokeBiometricsService;
import com.example.shop.services.KeystrokeBiometricsService.*;
import com.example.shop.services.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module 4 — Keystroke Biometrics Controller
 *
 * Endpoints:
 *   GET  /biometrics                    → demo page
 *   POST /biometrics/analyze            → analyze keystroke events → profile JSON
 *   POST /biometrics/enroll             → save baseline profile for a user
 *   POST /biometrics/verify             → compare new session against baseline
 *   GET  /biometrics/profile/{userId}   → get stored profile info
 *
 * In production, profiles would be persisted in the database.
 * Here we use an in-memory map for demo purposes (survives until server restart).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class KeystrokeBiometricsController {

    private final KeystrokeBiometricsService biometricsService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // In-memory profile store: userId → BiometricProfile
    // Replace with a JPA repository in production
    private static final Map<String, BiometricProfile> profileStore = new ConcurrentHashMap<>();

    private User currentUser(Principal principal, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        return principal != null ? userService.getUserByPrincipal(principal) : null;
    }

    @GetMapping("/biometrics")
    public String page(Model model, Principal principal, Authentication auth) {
        User user = currentUser(principal, auth);
        model.addAttribute("user", user);
        if (user != null) {
            boolean hasProfile = profileStore.containsKey(String.valueOf(user.getId()));
            model.addAttribute("hasProfile", hasProfile);
        }
        return "biometrics";
    }

    /**
     * Analyze a list of keystroke events and return a biometric profile.
     * Does NOT store anything — pure analysis.
     */
    @PostMapping("/biometrics/analyze")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyze(
            @RequestBody Map<String, Object> body,
            Principal principal, Authentication auth) {

        User user = currentUser(principal, auth);
        String userId = user != null ? String.valueOf(user.getId()) : "anonymous";

        try {
            List<Map<String, Object>> rawEvents = objectMapper.convertValue(
                    body.get("events"), new TypeReference<>() {});

            if (rawEvents == null || rawEvents.size() < 5) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Потрібно мінімум 5 натискань клавіш для аналізу"));
            }

            List<KeystrokeEvent> events = rawEvents.stream().map(e -> new KeystrokeEvent(
                    String.valueOf(e.get("key")),
                    ((Number) e.get("pressTime")).longValue(),
                    ((Number) e.get("releaseTime")).longValue(),
                    e.containsKey("positionInWord") ? ((Number) e.get("positionInWord")).intValue() : 0
            )).toList();

            BiometricProfile profile = biometricsService.analyze(userId, events);

            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("userId",          profile.userId());
            resp.put("avgDwellMs",      Math.round(profile.avgDwellMs() * 10) / 10.0);
            resp.put("stdDwellMs",      Math.round(profile.stdDwellMs() * 10) / 10.0);
            resp.put("avgFlightMs",     Math.round(profile.avgFlightMs() * 10) / 10.0);
            resp.put("stdFlightMs",     Math.round(profile.stdFlightMs() * 10) / 10.0);
            resp.put("typingSpeedCpm",  (double) Math.round(profile.typingSpeedCpm()));
            resp.put("errorRate",       Math.round(profile.errorRate() * 1000) / 10.0);
            resp.put("burstRatio",      Math.round(profile.burstRatio() * 1000) / 10.0);
            resp.put("fingerprintHash", profile.fingerprintHash());
            resp.put("uniquenessScore", Math.round(profile.uniquenessScore() * 10) / 10.0);
            resp.put("riskLevel",       profile.riskLevel());
            resp.put("digraphCount",    (double) profile.digraphTimings().size());
            resp.put("keystrokeCount",  (double) events.size());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Biometrics analyze error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Enroll: analyze events and save as baseline profile for this user.
     */
    @PostMapping("/biometrics/enroll")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enroll(
            @RequestBody Map<String, Object> body,
            Principal principal, Authentication auth) {

        User user = currentUser(principal, auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));

        String userId = String.valueOf(user.getId());

        try {
            List<Map<String, Object>> rawEvents = objectMapper.convertValue(
                    body.get("events"), new TypeReference<>() {});

            if (rawEvents == null || rawEvents.size() < 10) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Для реєстрації потрібно мінімум 10 натискань клавіш"));
            }

            List<KeystrokeEvent> events = rawEvents.stream().map(e -> new KeystrokeEvent(
                    String.valueOf(e.get("key")),
                    ((Number) e.get("pressTime")).longValue(),
                    ((Number) e.get("releaseTime")).longValue(),
                    e.containsKey("positionInWord") ? ((Number) e.get("positionInWord")).intValue() : 0
            )).toList();

            BiometricProfile profile = biometricsService.analyze(userId, events);

            if ("HIGH".equals(profile.riskLevel())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Виявлено аномальну поведінку. Введіть текст вручну природньо."));
            }

            profileStore.put(userId, profile);
            log.info("Biometric profile enrolled for user={}, hash={}", userId, profile.fingerprintHash());

            return ResponseEntity.ok(Map.of(
                    "enrolled",         true,
                    "fingerprintHash",  profile.fingerprintHash(),
                    "uniquenessScore",  Math.round(profile.uniquenessScore() * 10) / 10.0,
                    "typingSpeedCpm",   Math.round(profile.typingSpeedCpm()),
                    "riskLevel",        profile.riskLevel(),
                    "message",          "Поведінковий профіль збережено успішно!"
            ));

        } catch (Exception e) {
            log.error("Biometrics enroll error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify: compare a new typing session against the enrolled baseline.
     */
    @PostMapping("/biometrics/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, Object> body,
            Principal principal, Authentication auth) {

        User user = currentUser(principal, auth);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Не авторизовано"));

        String userId = String.valueOf(user.getId());
        BiometricProfile baseline = profileStore.get(userId);
        if (baseline == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Спочатку зареєструйте свій поведінковий профіль (/biometrics/enroll)"));
        }

        try {
            List<Map<String, Object>> rawEvents = objectMapper.convertValue(
                    body.get("events"), new TypeReference<>() {});

            List<KeystrokeEvent> events = rawEvents.stream().map(e -> new KeystrokeEvent(
                    String.valueOf(e.get("key")),
                    ((Number) e.get("pressTime")).longValue(),
                    ((Number) e.get("releaseTime")).longValue(),
                    e.containsKey("positionInWord") ? ((Number) e.get("positionInWord")).intValue() : 0
            )).toList();

            BiometricProfile candidate = biometricsService.analyze(userId + "_candidate", events);
            VerificationResult result  = biometricsService.verify(baseline, candidate);

            log.info("Biometric verify: user={}, similarity={}%, verdict={}",
                    userId, String.format("%.1f", result.similarityPct()), result.verdict());

            return ResponseEntity.ok(Map.of(
                    "isMatch",       result.isMatch(),
                    "similarity",    Math.round(result.similarityPct() * 10) / 10.0,
                    "verdict",       result.verdict(),
                    "explanation",   result.explanation(),
                    "baselineHash",  baseline.fingerprintHash(),
                    "candidateHash", candidate.fingerprintHash()
            ));

        } catch (Exception e) {
            log.error("Biometrics verify error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get stored profile info for a user (no sensitive raw data).
     */
    @GetMapping("/biometrics/profile/{userId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String userId) {
        BiometricProfile p = profileStore.get(userId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "userId",          p.userId(),
                "fingerprintHash", p.fingerprintHash(),
                "uniquenessScore", p.uniquenessScore(),
                "typingSpeedCpm",  p.typingSpeedCpm(),
                "riskLevel",       p.riskLevel(),
                "enrolled",        true
        ));
    }
}