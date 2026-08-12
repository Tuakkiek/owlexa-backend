package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    boolean existsByUser_IdAndCenter_Id(Long userId, Long centerId);

    List<Membership> findAllByCenter_Id(Long centerId);

    List<Membership> findAllByUser_Id(Long userId);

    List<Membership> findAllByCenter_IdAndUserRole(Long centerId, Role role);

    Optional<Membership> findByUser_IdAndCenter_Id(Long userId, Long centerId);

    Optional<Membership> findByUser_IdAndCenter_IdAndUserRole(
            Long userId,
            Long centerId,
            Role userRole
    );

    void deleteByCenter_Id(Long centerId);

    long countByCenter_IdAndUserRole(Long centerId, Role userRole);

    long countByCenter_Id(Long centerId);
}
