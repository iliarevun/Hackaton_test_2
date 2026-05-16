package com.example.shop.services;


import com.example.shop.enums.Role;
import com.example.shop.models.Avatar;
import com.example.shop.models.ConfirmationToken;
import com.example.shop.models.Image;
import com.example.shop.models.User;
import com.example.shop.repositories.AvatarRepository;
import com.example.shop.repositories.ConfirmationTokenRepository;
import com.example.shop.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarRepository avatarRepository;
    private final ConfirmationTokenService confirmationTokenService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    public boolean createUser(User user, MultipartFile fileAvatar) throws IOException {
        if(userRepository.findByEmail(user.getEmail()) != null)
            return false;

        Avatar imageAvatar;
        if(fileAvatar != null && fileAvatar.getSize() != 0){
            imageAvatar = toAvatarEntity(fileAvatar);
            assert imageAvatar != null;
            user.addAvatar(imageAvatar);
        }
        else{
           imageAvatar = new Avatar(user.getId(), avatarRepository.getReferenceById(0l).getName(),
                   avatarRepository.getReferenceById(0l).getOriginalFileName(),  avatarRepository.getReferenceById(0l).getSize(),
                   avatarRepository.getReferenceById(0l).getContentType(), avatarRepository.getReferenceById(0l).getBytes(),user);
//           imageAvatar.setName(avatarRepository.getReferenceById(0l).getName());
//           imageAvatar.setSize(avatarRepository.getReferenceById(0l).getSize());
//           imageAvatar.setContentType(avatarRepository.getReferenceById(0l).getContentType());
//           imageAvatar.setBytes(avatarRepository.getReferenceById(0l).getBytes());
//           imageAvatar.setOriginalFileName(avatarRepository.getReferenceById(0l).getOriginalFileName());
//
            user.setAvatar(imageAvatar);
            //user.addAvatar(imageAvatar);
        }


        user.setActive(false);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(Role.ROLE_USER);
        log.info("\u001B[31mSaving new User with email {}\u001B[0m", user.getEmail());
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), user);
        confirmationTokenService.saveConfirmationToken(confirmationToken);
        return true;

    }

    public void createUserFromOAuth2(Model model, OAuth2AuthenticationToken token) throws IOException {
        OAuth2User oAuth2User = token.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // If the user already exists, return the existing one
        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            if (existingUser.getRoles().contains(Role.ROLE_ADMIN)) {
                model.addAttribute("admin", true);
            }
            // If the user exists, we can simply log them in or return an appropriate message
            model.addAttribute("message", "Welcome back, " + existingUser.getName());
            return;
        }

        // If the user is new, create them
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(oAuth2User.getAttribute("name"));
        newUser.setPhoneNumber("Not specified");
        newUser.setActive(true);
        newUser.getRoles().add(Role.ROLE_ADMIN);

        // Create avatar
        Avatar userAvatar = new Avatar();
        String avatarUrl = oAuth2User.getAttribute("picture");
        if (avatarUrl != null) {
            try {
                URL url = new URL(avatarUrl);
                System.out.println("Downloading avatar from: " + avatarUrl);
                InputStream inputStream = url.openStream();
                byte[] bytes = IOUtils.toByteArray(inputStream);
                inputStream.close();

                userAvatar.setBytes(bytes);
                userAvatar.setSize((long) bytes.length); // Fixed! Set file size
                userAvatar.setContentType("image/jpeg");  // Or get correct content type from HTTP headers
                userAvatar.setOriginalFileName(email + ".jpg");

                avatarRepository.save(userAvatar); // Save avatar first
                newUser.setAvatar(userAvatar); // Потім прив’язуємо до користувача
            }
            catch (Exception e){
                System.out.println(e);
            }
        }
// Save new user
        newUser.setAvatar(userAvatar);
        userRepository.save(newUser);
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // Return UserDetails object understood by Spring Security
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // uses the already-hashed password from DB
                .disabled(!user.isActive())
                .authorities(user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name()))
                        .collect(Collectors.toList()))
                .build();
    }
    public User findUserByMnemonic(String mnemonic) {
        return userRepository.findByMnemonic(mnemonic).orElse(null);
    }
    public void processAndSetBiometricProfile(User user, String rawAttemptsJson) {
        if (rawAttemptsJson == null || rawAttemptsJson.isBlank()) return;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode attemptsNode = mapper.readTree(rawAttemptsJson);

            // Check if we received an array of 5 attempts
            if (!attemptsNode.isArray() || attemptsNode.size() < 5) return;

            // Collect data: Key (code) -> List of all recorded press durations
            Map<String, List<Long>> dwellTimesMap = new HashMap<>();

            // 1. Iterate over each attempt
            for (JsonNode attempt : attemptsNode) {
                if (attempt.isArray()) {
                    for (JsonNode event : attempt) {
                        String code = event.path("code").asText("");
                        long press = event.path("pressTime").asLong(0);
                        long release = event.path("releaseTime").asLong(0);
                        long dwellTime = release - press;

                        // Filter out anomalies and empty codes
                        if (!code.isBlank() && dwellTime > 0) {
                            dwellTimesMap.computeIfAbsent(code, k -> new ArrayList<>()).add(dwellTime);
                        }
                    }
                }
            }

            // 2. Calculate the arithmetic mean for each key
            Map<String, Long> finalAverages = new HashMap<>();

            // Fixed iteration over EntrySet with explicit type definitions
            for (Map.Entry<String, List<Long>> entry : dwellTimesMap.entrySet()) {
                String keycode = entry.getKey();
                List<Long> times = entry.getValue();

                if (times != null && !times.isEmpty()) {
                    long sum = 0;
                    for (Long t : times) {
                        sum += t;
                    }
                    long average = sum / times.size(); // Compute average for this key
                    finalAverages.put(keycode, average);
                }
            }

            // 3. Save the averaged biometric profile as JSON in the User entity
            String profileJson = mapper.writeValueAsString(finalAverages);
            user.setBiometricProfileJson(profileJson);
            user.setUseBiometricsWithPassword(true); // activate the password-link flag

            log.info("\u001B[32m[Biometrics] Profile for user {} successfully built from 5 attempts!\u001B[0m", user.getEmail());

        } catch (Exception e) {
            log.error("Error processing biometric training for user: {}", e.getMessage());
        }
    }

    public Avatar toAvatarEntity(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return null;
        }

        Avatar avatar = new Avatar();
        avatar.setName(file.getName());
        avatar.setOriginalFileName(file.getOriginalFilename());
        avatar.setContentType(file.getContentType());
        avatar.setSize(file.getSize());
        avatar.setBytes(file.getBytes());
        return avatar;

    }

    @Transactional
    public String confirmToken(String token) {
        log.info("\u001b[31mReceived token: " + token + "\u001b[0m");
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() ->
                        new IllegalStateException("token not found"));


        if (confirmationToken.getConfirmedAt() != null) {
            System.out.println("email already confirmed");
            log.info("\u001b[31memail already confirmed\u001b[0m");
            return "/login?confirmed";
        }


        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            log.info("\u001b[31mtoken expired\u001b[0m");
            Optional<User> userOptional = userRepository.findById(confirmationToken.getId());
            if (userOptional.isPresent()) {
                deleteUser(userOptional.get().getId());
            } else {
                log.warn("User with ID {} not found. Skipping deletion.", confirmationToken.getId());
            }
            return "/login?expired";

        }


        confirmationTokenService.setConfirmedAt(token);
        confirmationTokenRepository.save(confirmationToken);
        enableUser(confirmationToken.getUser().getEmail());


        confirmationToken.getUser().setActive(true);
        userRepository.save(confirmationToken.getUser());

        return "confirmation";
    }


    public List<User> list(){
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        List<ConfirmationToken> tokens = confirmationTokenRepository.findAllByUserId(id);
        if (!tokens.isEmpty()) {
            confirmationTokenRepository.deleteAll(tokens);
        }
        userRepository.deleteById(id);

        log.info("Deleted user with ID {}", id);
    }

    public void banUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if(user != null){
            if(user.isActive()) {
                user.setActive(false);
                log.info("\u001B[31mBan user with id {}; email {}\u001B[0m", user.getId(), user.getEmail());
            }
            else{
                user.setActive(true);
                log.info("\u001B[31mUnbanned user with id {}; email {}\u001B[0m", user.getId(), user.getEmail());
            }
        }
        userRepository.save(user);
    }

    public void changeUserRole(User user, String role){
        user.getRoles().clear();
        user.getRoles().add(Role.valueOf(role));

        userRepository.save(user);
    }

    public User getUserByPrincipal(Principal principal) {
        if(principal == null)
            return new User();

        return userRepository.findByEmail(principal.getName());
    }
    public void enableUser(String email) {
        userRepository.enableUser(email);
    }
    public User getUserByEmail(String email){return userRepository.findByEmail(email);}
    public User getUserById(Long id){
        return userRepository.findById(id).orElse(null);
    }

    public User findUserByPrincipal(String principalName) {

        return userRepository.findByEmail(principalName);
    }


    @Transactional
    public void save(User customer) {
        userRepository.save(customer);
    }
}
