package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.RegisterRequest;
import com.owlexa.owlexabackend.dto.response.AuthResponse;
import com.owlexa.owlexabackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest body) {
        return authService.login(body.phoneNumber(), body.password());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest body) {
        return authService.register(body);
    }

    public record LoginRequest(String phoneNumber, String password) {}
}
