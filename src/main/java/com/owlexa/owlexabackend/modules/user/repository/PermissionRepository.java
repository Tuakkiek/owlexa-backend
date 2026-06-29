package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByCodeIn(Collection<String> codes);
}
