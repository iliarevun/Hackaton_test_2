package com.example.shop.controller;

import com.example.shop.models.User;

import com.example.shop.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final UserService userService;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String API_KEY = "AIzaSyCd1swjms3HhteTmESxrMYpLvipKfZpN4c";

    private User me(Principal p, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        return p != null ? userService.getUserByPrincipal(p) : null;
    }

    /** GET /report  — weekly report page */
    @GetMapping("/report")
    public String reportPage(Model model, Principal p, Authentication auth) {
        User user = me(p, auth);
        return "";
    }

//    /** POST /report/ai-insight  — generate AI insight for the week */
//    @PostMapping("/report/ai-insight")
//    @ResponseBody
//    public Map<String, String> aiInsight(
//            @RequestBody(required = false) Map<String, Object> body,
//            Principal p, Authentication auth) {
//
//
//    }

    /** GET /report/data  — return JSON for Chart.js
    @GetMapping("/report/data")
    @ResponseBody
    public Map<String, Object> reportData(Principal p, Authentication auth) {

    }*/

}
