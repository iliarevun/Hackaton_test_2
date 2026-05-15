package com.example.shop.controller;

import com.example.shop.models.Avatar;
import com.example.shop.repositories.AvatarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayInputStream;


@Controller
@RequiredArgsConstructor
public class AvatarController {
    private final AvatarRepository avatarRepository;

    @GetMapping("/avatars/{id}")
    private ResponseEntity<?> getAvatarById(@PathVariable String id) {
        Long iD = Long.parseLong(id.replace("\u00A0", ""));
        Avatar avatar = avatarRepository.findById(iD).orElse(null);
        return ResponseEntity.ok()
                .header("fileName", avatar.getOriginalFileName())
                .contentType(MediaType.valueOf(avatar.getContentType()))
                .contentLength(avatar.getSize())
                .body(new InputStreamResource(new ByteArrayInputStream(avatar.getBytes())));
    }
}
