package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    Optional<User> findFirstByRole(Role role);

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
            @Param("role") Role role,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"userPermissions", "userPermission.permisison"})
    Optional<User> findWithUserPermissionById(Long id);

    @EntityGraph(attributePaths = {"userPermissions", "userPermission.permission"})
    Optional<User> findWithUserPermissionByPhoneNumber(String phoneNumber);

    /** Safe password update — avoids triggering cascade/orphanRemoval on userPermissions */
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    void updatePasswordById(@Param("id") Long id, @Param("password") String password);
}
