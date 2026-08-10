package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findFirstByRole(RoleName role);

    long countByRole(RoleName role);
}
