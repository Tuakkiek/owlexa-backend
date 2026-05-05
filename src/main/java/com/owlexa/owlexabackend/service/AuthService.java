package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.RegisterRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.repository.UserRepository;
import com.owlexa.owlexabackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String login(String phoneNumber, String password) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        String role = user.getRole() != null ? user.getRole().name() : null;
        return jwtUtil.generateToken(user.getPhoneNumber(), role);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new DuplicateResourceException("phoneNumber already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("email already exists");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRoleName().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("roleName must be OWNER|TEACHER|STUDENT");
        }

        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getPhoneNumber(), role.name());
        return AuthResponse.builder()
                .token(token)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(role.name())
                .centerName(null)
                .build();
    }
}
