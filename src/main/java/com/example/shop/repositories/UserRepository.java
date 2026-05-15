package com.example.shop.repositories;


import com.example.shop.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long>{
    User findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = true WHERE u.email = ?1")
    int enableUser(String email);
}

