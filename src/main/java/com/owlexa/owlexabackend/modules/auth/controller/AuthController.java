package com.owlexa.owlexabackend.modules.auth.controller;

import com.owlexa.owlexabackend.common.exception.AuthSessionException;
import com.owlexa.owlexabackend.common.util.CookieUtil;
import com.owlexa.owlexabackend.modules.auth.dto.request.LoginRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.modules.auth.dto.response.AuthResponse;
import com.owlexa.owlexabackend.modules.auth.dto.response.RefreshTokenResponse;
import com.owlexa.owlexabackend.modules.auth.dto.response.SessionResponse;
import com.owlexa.owlexabackend.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginResult result = authService.login(body, request);
        cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
        return result.getAuthResponse();
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = cookieUtil.extractRefreshTokenFromCookie(request)
                    .orElseThrow(() -> new AuthSessionException("Refresh token cookie is missing or expired"));
            AuthService.RefreshResult result = authService.refreshToken(refreshToken);
            if (result.isNewRefreshTokenGenerated()) {
                cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
            }
            return RefreshTokenResponse.builder()
                    .refreshToken(result.getRefreshToken())
                    .auth(result.getAuthResponse())
                    .build();
        } catch (AuthSessionException ex) {
            cookieUtil.clearRefreshTokenCookie(response);
            throw ex;
        }
    }

    @PostMapping("/register/owner")
    public AuthResponse registerOwner(
            @Valid @RequestBody RegisterOwnerRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
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
        String phoneNumber = currentPhoneNumber();
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
