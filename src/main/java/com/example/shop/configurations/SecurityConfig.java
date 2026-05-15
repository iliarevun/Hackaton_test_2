package com.example.shop.configurations;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(req -> req
                        .requestMatchers(
                                "/", "/registration", "/login", "/confirm",
                                "/game.css", "/style.css", "/script.js",
                                "/*.png", "/*.jpg", "/*.gif", "/*.svg", "/*.ico",
                                "/avatars/**", "/images/**", "/img/**",
                                "/setup", "/leaderboard", "/report", "/challenges", "/games", "/game/muscles", "/game/trainer", "/game/zones",
                                "/marketplace", "/exchange", "/repair", "/referal", "/reuse",
                                "/payment/result", "/user/**",
                                "/media-analysis", "/media-analysis/**",
                                "/query-proxy", "/query-proxy/**",
                                "/steganography", "/steganography/**",
                                "/biometrics", "/biometrics/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(f -> f
                        .loginPage("/login")
                        .permitAll()
                        .defaultSuccessUrl("/", true)
                )
                .logout(l -> l
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .oauth2Login(oauth2Login->{
                    oauth2Login.loginPage("/login")
                            .successHandler(((request, response, authentication) -> response.sendRedirect("/")));})
                .csrf(withDefaults())
                .csrf(c -> c.ignoringRequestMatchers(
                        "/payment", "/payment/result",
                        "/mental/analyze", "/mental/test/save", "/mental/test/deep-analysis",
                        "/mental/xp", "/mental/mood/save", "/mental/week-analysis",
                        "/physical/xp", "/physical/workout-done", "/physical/location",
                        "/progress/location",
                        "/game/xp",
                        "/api/challenges/send",
                        "/api/challenges/*/accept",
                        "/api/challenges/*/decline",
                        "/api/challenges/*/progress",
                        "/report/ai-insight",
                        // Ecosystem modules
                        "/media-analysis/analyze", "/media-analysis/upload",
                        "/query-proxy/ask", "/query-proxy/sanitize",
                        "/steganography/encrypt", "/steganography/decrypt",
                        "/biometrics/analyze", "/biometrics/enroll", "/biometrics/verify"
                ));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }
}
