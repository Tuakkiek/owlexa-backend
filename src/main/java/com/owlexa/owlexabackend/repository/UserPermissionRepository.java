package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission,Long>{

    boolean existsByUserIdAndPermissionCode(
            Long userId,
            String permissionCode
    );

}