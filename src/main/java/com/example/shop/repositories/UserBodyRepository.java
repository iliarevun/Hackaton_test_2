package com.example.shop.repositories;

import com.example.shop.models.UserBody;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserBodyRepository extends JpaRepository<UserBody, Long> {
    Optional<UserBody> findFirstByUserIdOrderByIdDesc(Long userId);
}
