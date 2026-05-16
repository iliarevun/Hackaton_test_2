package com.example.shop.controller;


import com.example.shop.enums.Role;
import com.example.shop.models.User;

import com.example.shop.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
/*@PreAuthorize("hasAuthority('ROLE_ADMIN')")*/
public class AdminController {

    private final UserService userService;

    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication, Principal principal){
        model.addAttribute("users", userService.list());

        User user = null;
        if (authentication != null) {

            if (authentication instanceof OAuth2AuthenticationToken token) {
                user = userService.getUserByEmail(token.getPrincipal().getAttribute("email"));


                model.addAttribute("user", user);

            } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
                user = userService.findUserByPrincipal(principal.getName());



                model.addAttribute("user", user);

            }

        }
            return "admin";
    }

    @PostMapping("/admin/user/ban/{id}")
    public String userBan(@PathVariable("id") String id){
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        userService.banUser(iD);
        return "redirect:/admin";
    }

    @GetMapping("/admin/user/edit/{user}")
    public String userEdit(@PathVariable("user") User user, Model model){
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "user_edit";
    }

    @PostMapping("admin/user/edit")
    public String userEdit(@RequestParam("userId") User user,  @RequestParam String role, Model model){
        userService.changeUserRole(user, role);
        return "redirect:/admin";
    }

    @PostMapping("admin/user/delete/{userId}")
    public String deleteUser(@PathVariable("userId") String id){
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        userService.deleteUser(iD);
        return "redirect:/admin";
    }

}
