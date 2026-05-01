package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest body) {
        return authService.login(body.phoneNumber(), body.password());
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest body) {
        return authService.register(body.phoneNumber(), body.password());
    }

    public record LoginRequest(String phoneNumber, String password) {}

    public record RegisterRequest(String phoneNumber, String password) {}
}
