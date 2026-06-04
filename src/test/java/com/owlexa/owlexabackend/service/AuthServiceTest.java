package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.repository.UserRepository;
import com.owlexa.owlexabackend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStudent_shouldCreateStudentAccount() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setPhoneNumber("0901234567");
        request.setEmail("student@example.com");
        request.setFullName("Nguyen Van A");
        request.setPassword("123456");

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(jwtUtil.generateRefreshToken("0901234567")).thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken("0901234567", "STUDENT")).thenReturn("access-token");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthResponse response = authService.registerStudent(request);

        assertThat(response.getPhoneNumber()).isEqualTo("0901234567");
        assertThat(response.getEmail()).isEqualTo("student@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getRoleName()).isEqualTo(Role.STUDENT.name());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void registerStudent_whenPhoneNumberExists_shouldThrowDuplicateException() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setPhoneNumber("0901234567");
        request.setEmail("student@example.com");
        request.setFullName("Nguyen Van A");
        request.setPassword("123456");

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStudent(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("phoneNumber");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenPasswordIsInvalid_shouldThrowBadRequestException() {
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setPassword("encoded-password");
        user.setRole(Role.STUDENT);

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login("0901234567", "wrong-password"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password");

        verify(userRepository, never()).save(any(User.class));
    }
}