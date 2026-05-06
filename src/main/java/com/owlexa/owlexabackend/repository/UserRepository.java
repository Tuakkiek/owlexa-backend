package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
