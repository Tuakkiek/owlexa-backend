package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.RefreshTokenRequest;
import com.owlexa.owlexabackend.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
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
    public AuthResponse login(String phoneNumber, String password) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow( () -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password");
        }

        String role = user.getRole() != null ? user.getRole().name() : null;

        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());
        String accessToken = jwtUtil.generateAccessToken(user.getPhoneNumber(), role);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiredAt(java.time.LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(role)
                .centerName(null)
                .build();

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

        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiredAt(java.time.LocalDateTime.now().plusDays(7));

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(phoneNumber, role.name());

        return AuthResponse.builder()
                .refreshToken(refreshToken)
                .accessToken(accessToken)
                .phoneNumber(phoneNumber)
                .email(email)
                .fullName(fullName)
                .roleName(role.name())
                .centerName(null)
                .build();
    }

    // REFRESH TOKEN
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BadRequestException("Refresh token must not be empty");
        }
        try {
            String refreshToken = request.getRefreshToken();

            // Check if it's refreshToken
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BadRequestException("Invalid token type");
            }

            // Extract subject(phoneNumber)
            String phoneNumber = jwtUtil.extractSubject(refreshToken);

            // Find user by phoneNumber
            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow( () -> new ResourceNotFoundException("User not found"));

            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
                throw new BadRequestException("Refresh token is not recognized");
            }

            if (user.getRefreshToken() == null ||
                    user.getRefreshTokenExpiredAt().isBefore(java.time.LocalDateTime.now())) {
                throw new BadRequestException("Refresh token has expired");
            }

            String role = user.getRole() != null ? user.getRole().name() : null;
            String newAccessToken = jwtUtil.generateAccessToken(phoneNumber, role);

            return AuthResponse.builder()
                    .refreshToken(refreshToken)
                    .accessToken(newAccessToken)
                    .phoneNumber(user.getPhoneNumber())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .roleName(role)
                    .centerName(null)
                    .build();
        }
        catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
    }

    // HELPER
    private String normalizeOptionalEmail(String email) {
        if (email == null) return null;

        String trimmed = email.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
