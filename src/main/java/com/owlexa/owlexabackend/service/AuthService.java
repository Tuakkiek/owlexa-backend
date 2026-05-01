package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.RoleRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import com.owlexa.owlexabackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String login(String phoneNumber, String password) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String role = user.getRole() != null ? user.getRole().getName() : null;
        return jwtUtil.generateToken(user.getPhoneNumber(), role);
    }

    public String register(String phoneNumber, String password) {
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new RuntimeException("Phone number already registered");
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(DEFAULT_ROLE);
                    return roleRepository.save(newRole);
                });

        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);

        return jwtUtil.generateToken(user.getPhoneNumber(), role.getName());
    }
}
