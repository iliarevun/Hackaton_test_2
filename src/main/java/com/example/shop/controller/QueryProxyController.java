package com.example.shop.controller;

import com.example.shop.models.User;
import com.example.shop.services.QueryProxyService;
import com.example.shop.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Module 2 — Query Proxy Controller
 * Endpoints:
 *   GET  /query-proxy            → page
 *   POST /query-proxy/ask        → sanitize + ask AI (JSON)
 *   POST /query-proxy/sanitize   → only sanitize, no AI call (JSON)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class QueryProxyController {

    private final QueryProxyService queryProxyService;
    private final UserService userService;

    private User currentUser(Principal principal, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        return principal != null ? userService.getUserByPrincipal(principal) : null;
    }

    @GetMapping("/query-proxy")
    public String page(Model model, Principal principal, Authentication auth) {
        model.addAttribute("user", currentUser(principal, auth));
        return "query_proxy";
    }

    /** Full pipeline: sanitize → ask AI → restore */
    @PostMapping("/query-proxy/ask")
    @ResponseBody
    public Map<String, Object> ask(
            @RequestBody Map<String, String> body,
            Principal principal, Authentication auth) {

        String query   = body.getOrDefault("query", "").trim();
        String context = body.getOrDefault("systemContext", "").trim();

        if (query.isBlank()) return Map.of("error", "Запит порожній");

        log.info("QueryProxy request: user={}, queryLength={}",
                currentUser(principal, auth) != null
                        ? currentUser(principal, auth).getId() : "anonymous",
                query.length());

        return queryProxyService.proxyQuery(query, context);
    }

    /** Only sanitize — preview what PII would be removed */
    @PostMapping("/query-proxy/sanitize")
    @ResponseBody
    public Map<String, Object> sanitizeOnly(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "").trim();
        if (query.isBlank()) return Map.of("error", "Запит порожній");

        QueryProxyService.SanitizedQuery sq = queryProxyService.sanitize(query);
        return Map.of(
                "original",          query,
                "sanitized",         sq.sanitized(),
                "replacementsCount", sq.replacements().size(),
                "detectedPII",       sq.replacements().values().stream().toList(),
                "tokens",            sq.replacements().keySet().stream().toList()
        );
    }
}
