package com.example.shop.services;


import com.example.shop.enums.Role;
import com.example.shop.models.Avatar;
import com.example.shop.models.ConfirmationToken;
import com.example.shop.models.Image;
import com.example.shop.models.User;
import com.example.shop.repositories.AvatarRepository;
import com.example.shop.repositories.ConfirmationTokenRepository;
import com.example.shop.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        if(fileAvatar.getSize() != 0){
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

        // Якщо користувач вже існує, повертаємо існуючого
        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            if (existingUser.getRoles().contains(Role.ROLE_ADMIN)) {
                model.addAttribute("admin", true);
            }
            // Якщо користувач існує, можемо просто логінити його або повернути відповідне повідомлення
            model.addAttribute("message", "Ласкаво просимо назад, " + existingUser.getName());
            return;
        }

        // Якщо користувач новий, створюємо його
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(oAuth2User.getAttribute("name"));
        newUser.setPhoneNumber("Не вказаний");
        newUser.setActive(true);
        newUser.getRoles().add(Role.ROLE_ADMIN);
        newUser.setCoins(100);

        // Створення аватара
        Avatar userAvatar = new Avatar();
        String avatarUrl = oAuth2User.getAttribute("picture");
        if (avatarUrl != null) {
            try {
                URL url = new URL(avatarUrl);
                System.out.println("Завантаження аватара з: " + avatarUrl);
                InputStream inputStream = url.openStream();
                byte[] bytes = IOUtils.toByteArray(inputStream);
                inputStream.close();

                userAvatar.setBytes(bytes);
                userAvatar.setSize((long) bytes.length); // Виправлено! Установка розміру файлу
                userAvatar.setContentType("image/jpeg");  // Або отримати коректний тип контенту з заголовків HTTP
                userAvatar.setOriginalFileName(email + ".jpg");

                avatarRepository.save(userAvatar); // Спочатку зберігаємо аватар
                newUser.setAvatar(userAvatar); // Потім прив’язуємо до користувача
            }
            catch (Exception e){
                System.out.println(e);
            }
        }
// Збереження нового користувача
        newUser.setAvatar(userAvatar);
        userRepository.save(newUser);
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
