package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {
    Optional<Center> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);
}

