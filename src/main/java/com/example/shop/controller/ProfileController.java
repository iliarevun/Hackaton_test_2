package com.example.shop.controller;

import com.example.shop.models.*;
import com.example.shop.repositories.*;
import com.example.shop.services.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final UserBodyRepository userBodyRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal, Authentication auth) {
        User user = getUser(principal, auth);
        if (user == null) return "redirect:/login";



        model.addAttribute("user", user);
        return "profile_game";
    }

    private User getUser(Principal principal, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken t)
            return userService.getUserByEmail(t.getPrincipal().getAttribute("email"));
        if (principal != null)
            return userService.findUserByPrincipal(principal.getName());
        return null;
    }
}
