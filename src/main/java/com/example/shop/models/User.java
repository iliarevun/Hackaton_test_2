package com.example.shop.models;

import com.example.shop.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "active")
    private boolean isActive;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "image_id")
    private Avatar avatar;

    @Column(name = "password", length = 1000)
    private String password;

    @Column(name = "coins")
    private double coins;

    @Column(name = "plastic_saved, kg")
    private double plasticSaved;

    @Column(name = "metal_saved, kg")
    private double metalSaved;

    @Column(name = "glass_saved, kg")
    private double glassSaved;

    @Column(name = "CO2_saved, kg")
    private double CO2Saved;

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
    private LocalDateTime dateOfRegistration;


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", avatar=" + avatar +
                ", password='" + password + '\'' +
                ", coins=" + coins +
                ", roles=" + roles +
                ", dateOfRegistration=" + dateOfRegistration +
                '}';
    }

    public void addAvatar(Avatar avatar){
        this.avatar = avatar;
        avatar.setUser(this);

    }
    //security
    @PrePersist
    private void init() {
        dateOfRegistration = LocalDateTime.now();
    }

    public boolean isAdmin(){return roles.contains(Role.ROLE_ADMIN);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return isActive == user.isActive && Double.compare(coins, user.coins) == 0 && Double.compare(plasticSaved, user.plasticSaved) == 0 && Double.compare(metalSaved, user.metalSaved) == 0 && Double.compare(glassSaved, user.glassSaved) == 0 && Double.compare(CO2Saved, user.CO2Saved) == 0 && Objects.equals(id, user.id) && Objects.equals(email, user.email) && Objects.equals(phoneNumber, user.phoneNumber) && Objects.equals(name, user.name) && Objects.equals(avatar, user.avatar) && Objects.equals(password, user.password) && Objects.equals(roles, user.roles) && Objects.equals(dateOfRegistration, user.dateOfRegistration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, phoneNumber, name, isActive, avatar, password, coins, plasticSaved, metalSaved, glassSaved, CO2Saved, roles, dateOfRegistration);
    }

    //Цей метод відповідає за можливість банити користувача;
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
