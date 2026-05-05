package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // LOGIN
    public String login(String phoneNumber, String password) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        String role = user.getRole() != null ? user.getRole().name() : null;
        return jwtUtil.generateToken(user.getPhoneNumber(), role);
    }

    // REGISTER STUDENT
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        return registerUser(
                request.getPhoneNumber(),
                request.getEmail(),
                request.getFullName(),
                request.getPassword(),
                Role.STUDENT
        );
    }

    // REGISTER OWNER
    public AuthResponse registerOwner(RegisterOwnerRequest request) {
        return registerUser(
                request.getPhoneNumber(),
                request.getEmail(),
                request.getFullName(),
                request.getPassword(),
                Role.OWNER
        );
    }

    // REGISTER USER
    public AuthResponse registerUser(
            String phoneNumber,
            String email,
            String fullName,
            String rawPassword,
            Role role
    ) {

        // Check duplicate phone number
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("phoneNumber is already exists");
        }

        // Check duplicate email
        String normalizeEmail = normalizeOptionalEmail(email);
        if (normalizeEmail != null && userRepository.existsByEmail(normalizeEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getPhoneNumber(), role.name());
        return AuthResponse.builder()
                .token(token)
                .phoneNumber(phoneNumber)
                .email(email)
                .fullName(fullName)
                .roleName(role.name())
                .centerName(null)
                .build();
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) return null;

        String trimmed = email.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
