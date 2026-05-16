package com.example.shop.configurations;

import com.example.shop.models.Avatar;
import com.example.shop.models.User;
import com.example.shop.enums.Role;
import com.example.shop.repositories.AvatarRepository;
import com.example.shop.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;

/**
 * Intercepts every OAuth2 login.
 * If the user doesn't exist yet → creates them in the DB.
 * If they do exist → just returns the existing user.
 * This prevents the ?error redirect that happens when
 * CustomUserDetailsService can't find the user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String picture  = oAuth2User.getAttribute("picture"); // Google avatar URL

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 login: email is null, skipping user creation");
            return oAuth2User;
        }

        // Already registered — nothing to do
        if (userRepository.findByEmail(email) != null) {
            log.debug("OAuth2 login: existing user {}", email);
            return oAuth2User;
        }

        // New user — create
        log.info("OAuth2 login: creating new user {}", email);
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null ? name : email.split("@")[0]);
        newUser.setPhoneNumber("Not specified");
        newUser.setActive(true);
        newUser.getRoles().add(Role.ROLE_USER);

        // Download and save avatar from Google
        if (picture != null) {
            try {
                InputStream is = new URL(picture).openStream();
                byte[] bytes = IOUtils.toByteArray(is);
                is.close();

                Avatar avatar = new Avatar();
                avatar.setBytes(bytes);
                avatar.setSize((long) bytes.length);
                avatar.setContentType("image/jpeg");
                avatar.setOriginalFileName(email + ".jpg");
                avatarRepository.save(avatar);
                newUser.setAvatar(avatar);
            } catch (Exception e) {
                log.warn("Could not download OAuth2 avatar: {}", e.getMessage());
            }
        }

        userRepository.save(newUser);
        log.info("OAuth2 login: user {} created successfully", email);

        return oAuth2User;
    }
}
