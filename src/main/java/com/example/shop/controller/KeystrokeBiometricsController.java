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
                        "error", "At least 5 keystrokes are required for analysis"));
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
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String userId = String.valueOf(user.getId());

        try {
            List<Map<String, Object>> rawEvents = objectMapper.convertValue(
                    body.get("events"), new TypeReference<>() {});

            if (rawEvents == null || rawEvents.size() < 10) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "At least 10 keystrokes are required for enrollment"));
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
                        "error", "Anomalous behavior detected. Please type naturally by hand."));
            }

            profileStore.put(userId, profile);
            log.info("Biometric profile enrolled for user={}, hash={}", userId, profile.fingerprintHash());

            return ResponseEntity.ok(Map.of(
                    "enrolled",         true,
                    "fingerprintHash",  profile.fingerprintHash(),
                    "uniquenessScore",  Math.round(profile.uniquenessScore() * 10) / 10.0,
                    "typingSpeedCpm",   Math.round(profile.typingSpeedCpm()),
                    "riskLevel",        profile.riskLevel(),
                    "message",          "Behavioral profile saved successfully!"
            ));

        } catch (Exception e) {
            log.error("Biometrics enroll error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify: compare a new typing session against the enrolled baseline.
     * Works for both authenticated users (DB profile) and anonymous (in-memory).
     */
    @PostMapping("/biometrics/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, Object> body,
            Principal principal, Authentication auth) {

        User user = currentUser(principal, auth);

        // Anonymous on the login page — no profile to compare against
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please enroll your behavioral profile first (/biometrics/enroll)"));
        }

        String userId = String.valueOf(user.getId());
        BiometricProfile baseline = profileStore.get(userId);

        // Fallback: try to load profile from DB if not in memory
        if (baseline == null && user.getBiometricProfileJson() != null && !user.getBiometricProfileJson().isBlank()) {
            try {
                Map<String, Object> stored = objectMapper.readValue(
                        user.getBiometricProfileJson(), new TypeReference<>() {});
                log.info("Loaded biometric baseline from DB for user={}", userId);
                // Reconstruct a lightweight baseline from stored averages for comparison
                // We'll use the stored data to create a synthetic profile
                baseline = biometricsService.reconstructFromJson(stored, userId);
                if (baseline != null) profileStore.put(userId, baseline);
            } catch (Exception ex) {
                log.warn("Could not reconstruct biometric profile from DB: {}", ex.getMessage());
            }
        }

        if (baseline == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please enroll your behavioral profile first (/biometrics/enroll)"));
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
     * Verify seed phrase — used on the login page (no auth required).
     * Checks the submitted phrase against the stored mnemonic in the DB.
     */
    @PostMapping("/biometrics/verify-seed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifySeed(
            @RequestBody Map<String, Object> body) {

        String seedPhrase = String.valueOf(body.getOrDefault("seedPhrase", "")).trim()
                .replaceAll("\\s+", " ");

        if (seedPhrase.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Seed phrase is empty"));
        }

        User user = userService.findUserByMnemonic(seedPhrase);
        if (user == null) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        return ResponseEntity.ok(Map.of("valid", true, "email", user.getEmail()));
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