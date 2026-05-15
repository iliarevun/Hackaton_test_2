package com.example.shop.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="user_data")
@Data
public class UserData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "city")
    private String city;

    @Column(name = "warehouse_number")
    private int warehouseNumber;


}
