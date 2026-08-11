package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.auth.LoginRequest;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.UserRepository;
import com.owlexa.owlexabackend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void rejectsLoginForLockedAccountAfterPasswordVerification() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        User user = mock(User.class);
        when(users.findByPhoneNumber("0905555551")).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("bcrypt-hash");
        when(passwordEncoder.matches("password123", "bcrypt-hash")).thenReturn(true);
        when(user.isActive()).thenReturn(false);

        AuthService authService = new AuthService(users, passwordEncoder, jwtService);

        assertThatThrownBy(() -> authService.login(new LoginRequest(
                "0905555551", "password123", null, null)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên");
    }
}
