package com.example.shop.repositories;


import com.example.shop.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    User findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = true WHERE u.email = ?1")
    int enableUser(String email);


    @Query(value = "SELECT * FROM users WHERE mnemonic_phrase = ?1", nativeQuery = true)
    java.util.Optional<User> findByMnemonic(String mnemonic);
}

