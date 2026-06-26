package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.LoginRequest;
import com.owlexa.owlexabackend.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.dto.response.SessionResponse;
import com.owlexa.owlexabackend.service.AuthService;
import com.owlexa.owlexabackend.util.CookieUtil;
import com.owlexa.owlexabackend.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(body, request);
        cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
        return result.getAuthResponse();
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractRefreshTokenFromCookie(request)
            .orElseThrow(() -> new BadRequestException("Cookie không tồn tại hoặc đã hết hạn"));
        AuthService.RefreshResult result = authService.refreshToken(refreshToken);
        if (result.isNewRefreshTokenGenerated()) {
            cookieUtil.setRefreshTokenCookie(response, result.getNewRefreshToken());
        }
        return result.getAuthResponse();
    }

    @PostMapping("/register/student")
    public AuthResponse registerStudent(@Valid @RequestBody RegisterStudentRequest body,
                                        HttpServletRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.registerStudent(body, request);
        cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
        return result.getAuthResponse();
    }

    @PostMapping("/register/owner")
    public AuthResponse registerOwner(@Valid @RequestBody RegisterOwnerRequest body,
                                      HttpServletRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.registerOwner(body, request);
        cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
        return result.getAuthResponse();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = (String) request.getAttribute("currentSessionId");
        authService.logout(sessionId);
        cookieUtil.clearRefreshTokenCookie(response);
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
    public void revokeAllSessions(HttpServletResponse response) {
        authService.revokeAllSessions(currentPhoneNumber());
        cookieUtil.clearRefreshTokenCookie(response);
    }

    private String currentPhoneNumber() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}