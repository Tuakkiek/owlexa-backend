package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.auth.AuthResponse;
import com.owlexa.owlexabackend.dto.auth.LoginRequest;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.UserRepository;
import com.owlexa.owlexabackend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        String phoneNumber = request.phoneNumber().trim();
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadCredentialsException("Số điện thoại hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Số điện thoại hoặc mật khẩu không đúng");
        }

        String centerName = user.getCenter() == null ? null : user.getCenter().getName();
        Long centerId = user.getCenter() == null ? null : user.getCenter().getId();
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                centerName,
                centerId,
                List.of()
        );
    }
}
