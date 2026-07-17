package com.owlexa.owlexabackend.modules.user.repository;

import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.permission WHERE rp.role = :role")
    List<RolePermission> findAllByRole(@Param("role") Role role);
}
