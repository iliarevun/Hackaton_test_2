package com.example.shop.services;


import com.example.shop.models.ConfirmationToken;
import com.example.shop.repositories.ConfirmationTokenRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Getter
@Setter
public class ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;

    public void saveConfirmationToken(ConfirmationToken token){
        confirmationTokenRepository.save(token);
    }
    public Optional<ConfirmationToken> getToken(String token) {
        return confirmationTokenRepository.findByToken(token);
    }

    public int setConfirmedAt(String token) {
        return confirmationTokenRepository.updateConfirmedAt(
                token, LocalDateTime.now());
    }

    public ConfirmationToken getConfirmationToken(Long user_id){
        List<ConfirmationToken> list = confirmationTokenRepository.findAllByUserId(user_id);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getConfirmedAt()!=null) return list.get(i);
        }
        return list.get(0);
    }

}
