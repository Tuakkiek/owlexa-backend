package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission,Long>{

    boolean existsByUser_IdAndPermissionCode(
            Long userId,
            String permissionCode
    );

    @Query("SELECT up FROM UserPermission up JOIN FETCH up.permission WHERE up.user.id = :userId")
    List<UserPermission> findAllByUser_Id(@Param("userId") Long userId);

    @Query("SELECT up FROM UserPermission up JOIN FETCH up.permission WHERE up.user.id = :userId AND up.permission.code = :code")
    java.util.Optional<UserPermission> findByUser_IdAndPermission_Code(
            @Param("userId") Long userId,
            @Param("code") String code
    );

    void deleteByUser_Id(Long userId);

}