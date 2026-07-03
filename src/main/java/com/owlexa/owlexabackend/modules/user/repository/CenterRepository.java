package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {

    boolean existsBySubdomain(String subdomain);

    Optional<Center> findBySubdomain(String subdomain);

    List<Center> findAllByOwner_Id(Long ownerId);
}
