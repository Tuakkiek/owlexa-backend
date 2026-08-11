package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findFirstByRole(RoleName role);

    long countByRole(RoleName role);

    long countByCenter_Id(Long centerId);

    @EntityGraph(attributePaths = "center")
    @Query("""
            select user from User user
            where (:role is null or user.role = :role)
              and (:search = ''
                   or lower(coalesce(user.fullName, '')) like lower(concat('%', :search, '%'))
                   or user.phoneNumber like concat('%', :search, '%')
                   or lower(coalesce(user.email, '')) like lower(concat('%', :search, '%')))
            """)
    Page<User> searchForAdmin(
            @Param("search") String search,
            @Param("role") RoleName role,
            Pageable pageable
    );
}
