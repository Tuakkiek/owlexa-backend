package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    boolean existsByUserIdAndCenterId(Long userId, Long centerId);

    List<Membership> findAllByCenterId(Long centerId);

    List<Membership> findAllByUserId(Long userId);

    List<Membership> findAllByCenterIdAndUserRole(Long centerId, Role role);

    Optional<Membership> findByUserIdAndCenterId(Long userId, Long centerId);

    Optional<Membership> findByUserIdAndCenterIdAndUserRole(
            Long userId,
            Long centerId,
            Role userRole
    );

    void deleteByCenterId(Long centerId);

    long countByCenterIdAndUserRole(Long centerId, Role userRole);
}
    