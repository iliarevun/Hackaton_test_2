package com.example.shop.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 4 — Keystroke Dynamics (Behavioral Biometrics)
 *
 * Analyzes typing rhythm (dwell time + flight time between keys) to create
 * a unique behavioral fingerprint for each user. This fingerprint is completely
 * different from passwords — it captures HOW you type, not WHAT you type.
 *
 * Terminology:
 *   - Dwell time:  how long a key is held down (key press → key release)
 *   - Flight time: gap between releasing one key and pressing the next (key release → next key press)
 *   - Digraph:     a pair of consecutive keystrokes with their timing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeystrokeBiometricsService {

    // ─────────────────────────────────────────────────────
    //  Core data record sent from the browser extension
    // ─────────────────────────────────────────────────────

    public record KeystrokeEvent(
            String key,          // key name (e.g., "a", "Space", "Backspace")
            long pressTime,      // epoch ms when key was pressed
            long releaseTime,    // epoch ms when key was released
            int  positionInWord  // index of this keystroke in the current word
    ) {}

    // ─────────────────────────────────────────────────────
    //  Computed biometric profile
    // ─────────────────────────────────────────────────────

    public record BiometricProfile(
            String userId,
            double avgDwellMs,          // average key hold duration
            double stdDwellMs,          // standard deviation of dwell time
            double avgFlightMs,         // average gap between keys
            double stdFlightMs,
            double typingSpeedCpm,      // characters per minute
            double errorRate,           // backspace ratio
            double burstRatio,          // fast consecutive keystrokes ratio
            Map<String, Double> digraphTimings,  // timings for common key pairs
            String fingerprintHash,     // SHA-256 hash of the profile
            double uniquenessScore,     // 0-100 uniqueness estimate
            String riskLevel            // LOW / MEDIUM / HIGH (anomaly detection)
    ) {}

    // ─────────────────────────────────────────────────────
    //  ANALYZE: compute biometric profile from raw events
    // ─────────────────────────────────────────────────────

    public BiometricProfile analyze(String userId, List<KeystrokeEvent> events) {
        if (events == null || events.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 keystrokes for analysis.");
        }

        // ── Dwell times ────────────────────────────────
        List<Double> dwells = events.stream()
                .map(e -> (double)(e.releaseTime() - e.pressTime()))
                .filter(d -> d >= 0 && d < 2000)  // sanity: 0–2000 ms
                .collect(Collectors.toList());

        double avgDwell = mean(dwells);
        double stdDwell = std(dwells, avgDwell);

        // ── Flight times ───────────────────────────────
        List<Double> flights = new ArrayList<>();
        for (int i = 1; i < events.size(); i++) {
            long flight = events.get(i).pressTime() - events.get(i - 1).releaseTime();
            if (flight >= -50 && flight < 3000) {  // allow slight overlap
                flights.add((double) flight);
            }
        }
        double avgFlight = mean(flights);
        double stdFlight = std(flights, avgFlight);

        // ── Typing speed (CPM) ─────────────────────────
        long totalTime = events.get(events.size() - 1).releaseTime() - events.get(0).pressTime();
        double cpm = totalTime > 0
                ? (events.size() / (totalTime / 60_000.0))
                : 0;

        // ── Error rate (backspace usage) ───────────────
        long backspaces = events.stream()
                .filter(e -> "Backspace".equals(e.key()))
                .count();
        double errorRate = (double) backspaces / events.size();

        // ── Burst ratio (very fast consecutive keys <80ms flight) ──
        long bursts = flights.stream().filter(f -> f < 80).count();
        double burstRatio = flights.isEmpty() ? 0 : (double) bursts / flights.size();

        // ── Digraph timings (most frequent key pairs) ──
        Map<String, List<Double>> digraphMap = new LinkedHashMap<>();
        for (int i = 1; i < events.size(); i++) {
            String digraph = events.get(i - 1).key() + "-" + events.get(i).key();
            long t = events.get(i).pressTime() - events.get(i - 1).pressTime();
            digraphMap.computeIfAbsent(digraph, k -> new ArrayList<>()).add((double) t);
        }
        Map<String, Double> digraphTimings = digraphMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)  // only repeated pairs
                .limit(20)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> mean(e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // ── Fingerprint hash ───────────────────────────
        String rawFingerprint = String.format("%.2f|%.2f|%.2f|%.2f|%.1f|%.3f|%.3f|%s",
                avgDwell, stdDwell, avgFlight, stdFlight, cpm, errorRate, burstRatio,
                digraphTimings.toString());
        String hash = sha256(rawFingerprint);

        // ── Uniqueness score ───────────────────────────
        // Based on standard deviation consistency — lower std = more consistent = more unique
        double uniqueness = computeUniqueness(stdDwell, stdFlight, cpm, errorRate);

        // ── Anomaly / risk detection ───────────────────
        String risk = detectRisk(avgDwell, avgFlight, cpm, errorRate);

        log.info("Biometric profile computed for user={}: cpm={}, avgDwell={}ms, risk={}",
                userId, Math.round(cpm), Math.round(avgDwell), risk);

        return new BiometricProfile(
                userId, avgDwell, stdDwell, avgFlight, stdFlight,
                cpm, errorRate, burstRatio, digraphTimings,
                hash, uniqueness, risk
        );
    }

    // ─────────────────────────────────────────────────────
    //  COMPARE: how similar are two profiles? (0-100%)
    // ─────────────────────────────────────────────────────

    public double compareProfiles(BiometricProfile baseline, BiometricProfile candidate) {
        // Weighted Manhattan distance across key metrics
        double dwellDiff   = Math.abs(baseline.avgDwellMs()  - candidate.avgDwellMs())  / 200.0;
        double flightDiff  = Math.abs(baseline.avgFlightMs() - candidate.avgFlightMs()) / 300.0;
        double speedDiff   = Math.abs(baseline.typingSpeedCpm() - candidate.typingSpeedCpm()) / 200.0;
        double errorDiff   = Math.abs(baseline.errorRate()   - candidate.errorRate());
        double burstDiff   = Math.abs(baseline.burstRatio()  - candidate.burstRatio());

        double distance = 0.30 * dwellDiff
                + 0.30 * flightDiff
                + 0.20 * speedDiff
                + 0.10 * errorDiff
                + 0.10 * burstDiff;

        double similarity = Math.max(0, 100 - distance * 100);
        log.debug("Profile comparison: {}% similarity", String.format("%.1f", similarity));
        return similarity;
    }

    // ─────────────────────────────────────────────────────
    //  VERIFY: is the candidate the same person as the baseline?
    // ─────────────────────────────────────────────────────

    public record VerificationResult(
            boolean isMatch,
            double similarityPct,
            String verdict,
            String explanation
    ) {}

    public VerificationResult verify(BiometricProfile baseline, BiometricProfile candidate) {
        double sim = compareProfiles(baseline, candidate);
        boolean isMatch = sim >= 70.0;
        String verdict = isMatch
                ? (sim >= 90 ? "STRONG_MATCH" : "MATCH")
                : (sim >= 50 ? "WEAK_MATCH" : "NO_MATCH");
        String explanation = switch (verdict) {
            case "STRONG_MATCH" -> "Поведінковий відбиток майже ідентичний. Висока впевненість.";
            case "MATCH"        -> "Достатня схожість. Ймовірно та сама людина.";
            case "WEAK_MATCH"   -> "Часткова схожість. Можлива інша умова (стрес, мобільний пристрій).";
            case "NO_MATCH"     -> "Поведінковий відбиток не збігається. Ймовірно інша людина.";
            default             -> "Невідомо.";
        };
        return new VerificationResult(isMatch, sim, verdict, explanation);
    }

    // ─────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────

    private double mean(List<Double> values) {
        if (values.isEmpty()) return 0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double std(List<Double> values, double mean) {
        if (values.size() < 2) return 0;
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double computeUniqueness(double stdDwell, double stdFlight, double cpm, double errorRate) {
        // Lower std = more consistent = easier to match = higher uniqueness signal
        double consistency = Math.max(0, 1 - (stdDwell / 200.0)) * 40
                + Math.max(0, 1 - (stdFlight / 300.0)) * 30;
        double speedFactor = Math.min(cpm / 400.0, 1.0) * 20;
        double errorFactor = Math.max(0, 1 - errorRate * 5) * 10;
        return Math.min(100, consistency + speedFactor + errorFactor);
    }

    private String detectRisk(double avgDwell, double avgFlight, double cpm, double errorRate) {
        // Suspicious if: extremely fast (bot-like), extremely slow, or very high error rate
        if (cpm > 600 || avgDwell < 20 || avgFlight < 10) return "HIGH";   // likely bot
        if (cpm < 10 || errorRate > 0.4) return "MEDIUM";                   // unusual human
        return "LOW";
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 16); // first 16 chars for display
        } catch (Exception e) {
            return "hash-error";
        }
    }
}