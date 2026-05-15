package com.example.shop.repositories;


import com.example.shop.models.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataRepository extends JpaRepository<UserData, Long> {

    UserData findByUserId(Long id);

}
