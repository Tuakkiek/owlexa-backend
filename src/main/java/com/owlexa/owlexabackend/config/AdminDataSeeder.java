package com.owlexa.owlexabackend.config;

import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminDataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String phoneNumber;
    private final String fullName;
    private final String email;
    private final String password;

    public AdminDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin.phone-number}") String phoneNumber,
            @Value("${app.seed.admin.full-name:System Administrator}") String fullName,
            @Value("${app.seed.admin.email:admin@owlexa.local}") String email,
            @Value("${app.seed.admin.password}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneNumber = phoneNumber.trim();
        this.fullName = fullName.trim();
        this.email = email.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existingAdmin = userRepository.findFirstByRole(RoleName.ADMIN);
        if (existingAdmin.isPresent()) {
            log.info("Admin seed đã tồn tại với số điện thoại {}", mask(existingAdmin.get().getPhoneNumber()));
            return;
        }

        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalStateException(
                    "Không thể seed Admin: số điện thoại cấu hình đã thuộc tài khoản khác");
        }

        User admin = new User(
                phoneNumber,
                fullName,
                email.isBlank() ? null : email,
                passwordEncoder.encode(password),
                RoleName.ADMIN
        );
        userRepository.save(admin);
        log.info("Đã seed tài khoản Admin với số điện thoại {}", mask(phoneNumber));
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
