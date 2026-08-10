package com.owlexa.owlexabackend.config;

import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AdminDataSeederTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsAdminWhenNoAdminExists() {
        when(userRepository.findFirstByRole(RoleName.ADMIN)).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("0900000000")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");

        AdminDataSeeder seeder = new AdminDataSeeder(
                userRepository,
                passwordEncoder,
                "0900000000",
                "System Administrator",
                "admin@owlexa.local",
                "password123"
        );
        seeder.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedAdmin = captor.getValue();
        assertThat(savedAdmin.getPhoneNumber()).isEqualTo("0900000000");
        assertThat(savedAdmin.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(savedAdmin.getRole()).isEqualTo(RoleName.ADMIN);
        assertThat(savedAdmin.getCenter()).isNull();
    }

    @Test
    void doesNotCreateOrResetAnExistingAdmin() {
        User existingAdmin = new User(
                "0999999999", "Existing Admin", null, "existing-hash", RoleName.ADMIN);
        when(userRepository.findFirstByRole(RoleName.ADMIN)).thenReturn(Optional.of(existingAdmin));

        AdminDataSeeder seeder = new AdminDataSeeder(
                userRepository,
                passwordEncoder,
                "0900000000",
                "System Administrator",
                "admin@owlexa.local",
                "password123"
        );
        seeder.run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}
