package com.example.shop.controller;


import com.example.shop.email.EmailSender;
import com.example.shop.enums.AdvertType;
import com.example.shop.models.*;
//import com.example.shop.repositories.AvatarRepository;
import com.example.shop.repositories.AvatarRepository;
import com.example.shop.repositories.UserRepository;
import com.example.shop.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;
   // private final NotificationService notificationService;

    @GetMapping("/login")
    public String login(){
        return "login";
    }


    @PostMapping("/login/mnemonic")
    public String loginByMnemonic(@RequestParam("mnemonicPhrase") String mnemonicPhrase,
                                  jakarta.servlet.http.HttpServletRequest request) {
        // Remove accidental double spaces from the phrase
        String cleanMnemonic = mnemonicPhrase.trim().replaceAll("\\s+", " ");

        // 1. Find the user via the service
        User user = userService.findUserByMnemonic(cleanMnemonic);

        if (user == null) {
            System.out.println("\u001B[31mMnemonic login failed: Seed phrase match not found.\u001B[0m");
            return "redirect:/login?error=mnemonic_invalid";
        }

        try {
            // 2. Load UserDetails using the found user's email
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    userService.loadUserByUsername(user.getEmail());

            // 3. Create the authentication token
            org.springframework.security.core.Authentication authentication =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

            // 4. Set authentication in SecurityContextHolder
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

            // 5. Save context in session so authentication persists when navigating to the main page
            request.getSession().setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    org.springframework.security.core.context.SecurityContextHolder.getContext()
            );

            System.out.println("\u001B[32mUser " + user.getEmail() + " successfully logged in via Seed Phrase\u001B[0m");
            return "redirect:/";

        } catch (Exception e) {
            System.out.println("\u001B[31mAuthentication setup error: " + e.getMessage() + "\u001B[0m");
            return "redirect:/login?error=true";
        }
    }

    @GetMapping("/registration")
    public String registration() {

        return "registration";
    }

    @GetMapping("/oauth2/authorization/google")
    public String createUserOAuth2(Model model, OAuth2AuthenticationToken token) throws IOException {
        System.out.println(token);
        if (token != null) {
            userService.createUserFromOAuth2(model, token);
        }
        return "work";
    }

    @PostMapping("/registration")
    public String createUser(
            @RequestParam(value = "fileAvatar", required = false) MultipartFile fileAvatar,
            @RequestParam(value = "seedPhrase",       required = false) String seedPhrase,
            @RequestParam(value = "biometricProfile", required = false) String biometricProfile,
            @RequestParam(value = "rawBiometricAttempts", required = false) String rawBiometricAttempts,
            User user, Model model) throws IOException {

        // Attach seed phrase (mnemonic) to user before saving
        if (seedPhrase != null && !seedPhrase.isBlank()) {
            user.setMnemonicPhrase(seedPhrase.trim().replaceAll("\\s+", " "));
        }

        // fileAvatar is optional — createUser handles null/empty correctly

        if (!userService.createUser(user, fileAvatar)) {
            model.addAttribute("errorMessage", "User with email "
                    + user.getEmail() + " already exists");
            return "registration";
        }

        // Save biometric profile if provided (from the 5-attempt enrollment)
        if (biometricProfile != null && !biometricProfile.isBlank()) {
            User saved = userRepository.findByEmail(user.getEmail());
            if (saved != null) {
                saved.setBiometricProfileJson(biometricProfile);
                saved.setUseBiometricsWithPassword(true);
                userRepository.save(saved);
            }
        } else if (rawBiometricAttempts != null && !rawBiometricAttempts.isBlank()) {
            User saved = userRepository.findByEmail(user.getEmail());
            if (saved != null) {
                userService.processAndSetBiometricProfile(saved, rawBiometricAttempts);
                userRepository.save(saved);
            }
        }

        ConfirmationToken confirmationToken = confirmationTokenService.getConfirmationToken(user.getId());
        String link = "https://hackathon-test-g54r.onrender.com/confirm?token=" + confirmationToken.getToken();

        String confirmMessage = "An email has been sent to your email address. " +
                "Follow the link in the email to confirm your email address.";

        emailSender.send(user.getEmail(), buildEmail(user.getName(), link));
        model.addAttribute("confirmMessage", confirmMessage);
        return "/login";
    }

    @PostMapping("/changeAvatar/{id}")
    public String changeAvatar(@RequestParam("file") MultipartFile file, @PathVariable String id) throws IOException {
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        User user = userRepository.findById(iD).orElse(null);

        
/*
       if(user.getAvatar() != null) {
            //if(user.getAvatar() != null) {
                //avatarRepository.deleteById(user.getAvatar().getId());
                user.getAvatar().setName(file.getName());
                user.getAvatar().setOriginalFileName(file.getOriginalFilename());
                user.getAvatar().setContentType(file.getContentType());
                user.getAvatar().setSize(file.getSize());
                user.getAvatar().setBytes(file.getBytes());
            //}
            //user.addAvatar(avatar);
            userRepository.save(user);
        }*/
        System.out.println("\u001B[31mProfile " + user.getEmail() + " avatar changed\u001B[0m");
        return "redirect:/";
    }


    // Security check: Access for authenticated and unauthenticated users.
    // When not logged in, should redirect to the login page;
    @GetMapping("/hello")
    public String securityUrl() {
        return "hello";
    }


    @GetMapping("/user/{user}")
    public String userInfo(@PathVariable("user") String id, Model model,
                           Principal principal, OAuth2AuthenticationToken token){
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        User user = userService.getUserById(iD);

        model.addAttribute("user", user);
        if (token==null)
            model.addAttribute("viewer", userService.findUserByPrincipal(principal.getName()));
        else
            model.addAttribute("viewer", userService.getUserByEmail(token.getPrincipal().getAttribute("email")));
/*
        model.addAttribute("euro_exchange_rate", currencyExchangeService.getEuroToUahRate());
        model.addAttribute("notifications", notificationService.getNotificationsList(user.getId()));*/
        return "user_info";
    }


    @PostMapping("/profile/{id}")
    public String profile(@PathVariable("id") String id, Model model){
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        User user = userService.getUserById(iD);
        
        model.addAttribute("user", user);
     //   model.addAttribute("euro_exchange_rate", currencyExchangeService.getEuroToUahRate());
     //   model.addAttribute("notifications", notificationService.getNotificationsList(user.getId()));
        model.addAttribute("advertTypes", AdvertType.values());
        return "profile";
    }



    @GetMapping("/confirm")
    public String confirm(@RequestParam("token") String token, Model model) {
        System.out.println("\u001B[31mToken received: " + token + "\u001b[0m");
        String tokenStatus = userService.confirmToken(token);

        switch (tokenStatus) {
            case "/login?confirmed":
                model.addAttribute("message_already_confirmed", "Your email address has already been confirmed. Please log in.");
                break;

            case "/login?expired":
                model.addAttribute("message_expired", "The confirmation link has expired. Please request a new one.");
                break;

            case "confirmation":
                model.addAttribute("message_confirmed", "Your email address has been successfully confirmed. Please log in.");
                break;

            default:
                model.addAttribute("message", "Unexpected error. Please contact support.");
                break;
        }

        return "login";
    }


    private String buildEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hello, " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering with SecureMind Hub. Please follow this link to confirm your email address: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate now</a> </p></blockquote>\n The link will expire in 15 minutes. <p>See you at SecureMind Hub!</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}
