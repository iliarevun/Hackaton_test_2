package com.example.shop.controller;

import com.example.shop.models.User;
import com.example.shop.services.SteganographyService;
import com.example.shop.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

/**
 * Module 3 — Steganography Controller
 *
 * GET  /steganography                  → page
 * POST /steganography/encrypt          → encrypt text/file → Gemini image + 30s key
 * POST /steganography/decrypt          → upload stego-image + key → plaintext/file
 * GET  /steganography/key-status?key=  → countdown check
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SteganographyController {

    private final SteganographyService steganographyService;
    private final UserService userService;

    private User currentUser(Principal principal, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        return principal != null ? userService.getUserByPrincipal(principal) : null;
    }

    @GetMapping("/steganography")
    public String page(Model model, Principal principal, Authentication auth) {
        model.addAttribute("user", currentUser(principal, auth));
        return "steganography";
    }

    /**
     * Encrypt text or file, embed in Gemini image, return stego PNG + one-time key.
     */
    @PostMapping("/steganography/encrypt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> encrypt(
            @RequestParam(value = "text",       required = false) String text,
            @RequestParam(value = "file",       required = false) MultipartFile file,
            @RequestParam(value = "imageTheme", defaultValue = "") String imageTheme,
            @RequestParam(value = "coverPhoto", required = false) MultipartFile coverPhoto,
            Principal principal, Authentication auth) {

        boolean hasText = text != null && !text.isBlank();
        boolean hasFile = file != null && !file.isEmpty();
        if (!hasText && !hasFile) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введіть текст або завантажте файл"));
        }

        try {
            byte[] fileBytes  = hasFile ? file.getBytes() : null;
            String fileName   = hasFile ? file.getOriginalFilename() : null;
            String payload    = hasText ? text : "";
            byte[] coverBytes = (coverPhoto != null && !coverPhoto.isEmpty()) ? coverPhoto.getBytes() : null;

            log.info("Encrypt: hasText={}, hasFile={}, hasCover={}, style={}", hasText, hasFile, coverBytes!=null, imageTheme);
            Map<String, Object> result = steganographyService.encryptAndEmbed(payload, fileBytes, fileName, imageTheme, coverBytes);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Encrypt error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Decrypt: user uploads the stego-PNG + enters the key.
     */
    @PostMapping("/steganography/decrypt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> decrypt(
            @RequestParam("image") MultipartFile image,
            @RequestParam("key")   String key,
            Principal principal, Authentication auth) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Завантажте зображення"));
        }
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Введіть ключ"));
        }

        try {
            Map<String, Object> result = steganographyService.decryptFromImage(image.getBytes(), key);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Decrypt error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Frontend polls this to update the countdown.
     */
    @GetMapping("/steganography/key-status")
    @ResponseBody
    public Map<String, Object> keyStatus(@RequestParam("key") String key) {
        return steganographyService.keyStatus(key);
    }
}
