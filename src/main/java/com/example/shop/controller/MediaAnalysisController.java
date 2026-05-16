package com.example.shop.controller;

import com.example.shop.models.User;
import com.example.shop.services.MediaAnalysisService;
import com.example.shop.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;

/**
 * Module 1 — Media Analysis Controller
 * Endpoints:
 *   GET  /media-analysis         → page
 *   POST /media-analysis/analyze → analyze plain text (JSON)
 *   POST /media-analysis/upload  → analyze uploaded text file (JSON)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MediaAnalysisController {

    private final MediaAnalysisService mediaAnalysisService;
    private final UserService userService;

    private User currentUser(Principal principal, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        return principal != null ? userService.getUserByPrincipal(principal) : null;
    }

    @GetMapping("/media-analysis")
    public String page(Model model, Principal principal, Authentication auth) {
        User user = currentUser(principal, auth);
        model.addAttribute("user", user);
        return "media_analysis";
    }

    /** Analyze plain text submitted as JSON body */
    @PostMapping("/media-analysis/analyze")
    @ResponseBody
    public Map<String, Object> analyzeText(
            @RequestBody Map<String, String> body,
            Principal principal, Authentication auth) {

        String text = body.getOrDefault("text", "").trim();
        if (text.isBlank()) {
            return Map.of("error", "Text is empty");
        }
        if (text.length() < 30) {
            return Map.of("error", "Text is too short for analysis (minimum 30 characters)");
        }

        log.info("Media analysis requested by user={}, textLength={}",
                currentUser(principal, auth) != null
                        ? currentUser(principal, auth).getId() : "anonymous",
                text.length());

        return mediaAnalysisService.analyzeMedia(text);
    }

    /** Analyze uploaded .txt / .html / .md file */
    @PostMapping("/media-analysis/upload")
    @ResponseBody
    public Map<String, Object> analyzeFile(
            @RequestParam("file") MultipartFile file,
            Principal principal, Authentication auth) {

        if (file.isEmpty()) return Map.of("error", "File is empty");

        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.isBlank()) return Map.of("error", "File contains no text");

            log.info("Media analysis (file upload): name={}, size={}",
                    file.getOriginalFilename(), file.getSize());

            Map<String, Object> result = mediaAnalysisService.analyzeMedia(text);
            result = new java.util.LinkedHashMap<>(result);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            return result;
        } catch (Exception e) {
            log.error("File analysis error: {}", e.getMessage());
            return Map.of("error", "File read error: " + e.getMessage());
        }
    }
}
