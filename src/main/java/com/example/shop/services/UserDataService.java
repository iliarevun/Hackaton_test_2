package com.example.shop.services;


import com.example.shop.models.UserData;
import com.example.shop.repositories.UserDataRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDataService {

    private final UserDataRepository userDataRepository;

    public void saveUserData(Long userId, String fullName, String phoneNumber, String city, int warehouseNumber) {
        UserData userData = new UserData();

        userData.setUserId(userId);
        userData.setFullName(fullName);
        userData.setPhoneNumber(phoneNumber);
        userData.setCity(city);
        userData.setWarehouseNumber(warehouseNumber);

        userDataRepository.save(userData);

    }


    public UserData getUserById(Long id) {
        return userDataRepository.findByUserId(id);
    }



}
