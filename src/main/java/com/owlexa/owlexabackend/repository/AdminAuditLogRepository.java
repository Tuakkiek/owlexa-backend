package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.AdminAuditAction;
import com.owlexa.owlexabackend.entity.AdminAuditLog;
import com.owlexa.owlexabackend.entity.AdminAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    @EntityGraph(attributePaths = "admin")
    @Query("""
            select log from AdminAuditLog log
            where (:targetType is null or log.targetType = :targetType)
              and (:action is null or log.action = :action)
              and (:search = ''
                   or lower(log.targetName) like lower(concat('%', :search, '%'))
                   or lower(log.reason) like lower(concat('%', :search, '%'))
                   or lower(coalesce(log.admin.fullName, '')) like lower(concat('%', :search, '%'))
                   or log.admin.phoneNumber like concat('%', :search, '%'))
            """)
    Page<AdminAuditLog> searchForAdmin(
            @Param("search") String search,
            @Param("targetType") AdminAuditTargetType targetType,
            @Param("action") AdminAuditAction action,
            Pageable pageable
    );
}
