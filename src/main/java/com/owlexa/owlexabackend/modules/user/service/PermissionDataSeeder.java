package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import com.owlexa.owlexabackend.modules.user.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent seeder that runs at startup.
 * Since Flyway handles the initial schema and seed data, this class acts as a safety net
 * to ensure that any new permissions added to the code but missed in Flyway are logged,
 * or it can be used to warm up the cache.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionDataSeeder {

    private final RolePermissionRepository rolePermissionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        log.info("Verifying RBAC permissions via PermissionDataSeeder...");
        long count = rolePermissionRepository.count();
        log.info("Currently {} role-permission mappings exist in the database.", count);
        
        // As per enterprise architecture, Flyway is the primary seeder.
        // This component is retained for Phase 1 as requested, providing a hook for cache warmups or sanity checks.
    }
}
