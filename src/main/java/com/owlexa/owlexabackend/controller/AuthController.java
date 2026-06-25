package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.LoginRequest;
import com.owlexa.owlexabackend.dto.request.RefreshTokenRequest;
import com.owlexa.owlexabackend.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.dto.response.SessionResponse;
import com.owlexa.owlexabackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request) {
        return authService.login(body, request);
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(@RequestBody RefreshTokenRequest body) {
        return authService.refreshToken(body);
    }

    @PostMapping("/register/student")
    public AuthResponse registerStudent(@Valid @RequestBody RegisterStudentRequest body,
                                        HttpServletRequest request) {
        return authService.registerStudent(body, request);
    }

    @PostMapping("/register/owner")
    public AuthResponse registerOwner(@Valid @RequestBody RegisterOwnerRequest body,
                                      HttpServletRequest request) {
        return authService.registerOwner(body, request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String sessionId = (String) request.getAttribute("currentSessionId");
        authService.logout(sessionId);
    }

    @GetMapping("/sessions")
    public List<SessionResponse> getSessions(HttpServletRequest request) {
        String phoneNumber    = currentPhoneNumber();
        String currentSession = (String) request.getAttribute("currentSessionId");
        return authService.getSessions(phoneNumber, currentSession);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@PathVariable String sessionId) {
        authService.revokeSession(currentPhoneNumber(), sessionId);
    }

    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAllSessions() {
        authService.revokeAllSessions(currentPhoneNumber());
    }

    private String currentPhoneNumber() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}