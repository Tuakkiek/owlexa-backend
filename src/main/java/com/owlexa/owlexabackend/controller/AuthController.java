package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.dto.request.RegisterStudentRequest;
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

    // LOGIN
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest body) {
        return authService.login(body.phoneNumber(), body.password());
    }
    // REGISTER
    // Student register
    @PostMapping("/register/student")
    public AuthResponse registerStudent(@Valid @RequestBody RegisterStudentRequest body) {
            return authService.registerStudent(body);
    }
    // Owner register
    @PostMapping("/register/owner")
    public AuthResponse registerOwner(@Valid @RequestBody RegisterOwnerRequest body) {
        return authService.registerOwner(body);
    }

    // Record Login request
    private record LoginRequest(String phoneNumber, String password) {}
}
