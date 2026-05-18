package com.owlexa.owlexabackend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "refresh_token_expiry_at")
    private LocalDateTime refreshTokenExpiredAt;
}