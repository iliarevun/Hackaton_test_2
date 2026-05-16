package com.example.shop.controller;

import com.example.shop.models.User;
import com.example.shop.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final UserService userService;

    @GetMapping("/")
    public String mainPage(Model model, Principal principal,
                           Authentication auth, HttpServletRequest request) {
        User user = getUser(principal, auth);
        model.addAttribute("user", user);

        // Progress stub (no UserProgress entity in this build)
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalXp", 0);
        progress.put("level", 1);
        model.addAttribute("progress", progress);

        // CSRF helpers for Freemarker
        try {
            CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrf != null) {
                model.addAttribute("csrfToken",         csrf.getToken());
                model.addAttribute("csrfParameterName", csrf.getParameterName());
            }
        } catch (Exception ignored) {}

        return "main";
    }

    private User getUser(Principal principal, Authentication auth) {
        try {
            if (auth instanceof OAuth2AuthenticationToken t) {
                User u = userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
                return u != null ? u : new User();
            }
            if (principal != null) {
                User u = userService.getUserByPrincipal(principal);
                return u != null ? u : new User();
            }
        } catch (Exception e) {
            log.warn("Could not resolve user: {}", e.getMessage());
        }
        return new User(); // anonymous — empty object, not null
    }
}
