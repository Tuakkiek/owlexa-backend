package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermission,Long>{

    boolean existsByUser_IdAndPermissionCode(
            Long userId,
            String permissionCode
    );

}