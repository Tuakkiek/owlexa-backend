package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {

    boolean existsBySubdomain(String subdomain);

    Optional<Center> findBySubdomain(String subdomain);

    List<Center> findAllByOwnerId(Long ownerId);
}
