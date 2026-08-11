package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.admin.AdminCenterResponse;
import com.owlexa.owlexabackend.dto.admin.AdminStatsResponse;
import com.owlexa.owlexabackend.dto.admin.AdminUserResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.AdminAuditLog;
import com.owlexa.owlexabackend.entity.AdminAuditAction;
import com.owlexa.owlexabackend.entity.AdminAuditTargetType;
import com.owlexa.owlexabackend.dto.admin.AdminAuditLogResponse;
import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.AdminAuditLogRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AdminServiceTest {
    @Test
    void returnsSystemWideCounts() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        when(users.count()).thenReturn(31L);
        when(users.countByRole(RoleName.OWNER)).thenReturn(1L);
        when(users.countByRole(RoleName.TEACHER)).thenReturn(5L);
        when(users.countByRole(RoleName.STUDENT)).thenReturn(20L);
        when(users.countByRole(RoleName.CASHIER)).thenReturn(2L);
        when(users.countByRole(RoleName.ADMIN)).thenReturn(1L);
        when(centers.count()).thenReturn(3L);

        AdminStatsResponse stats = service(users, centers).getStats();

        assertThat(stats).isEqualTo(new AdminStatsResponse(31, 1, 5, 20, 2, 1, 3));
    }

    @Test
    void searchesAndMapsUsersForAdmin() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        User teacher = mock(User.class);
        Center center = mock(Center.class);
        when(teacher.getId()).thenReturn(8L);
        when(teacher.getFullName()).thenReturn("Nguyễn Văn Nam");
        when(teacher.getPhoneNumber()).thenReturn("0905555551");
        when(teacher.getEmail()).thenReturn("teacher@owlexa.vn");
        when(teacher.getRole()).thenReturn(RoleName.TEACHER);
        when(teacher.getCenter()).thenReturn(center);
        when(teacher.isActive()).thenReturn(true);
        when(center.getId()).thenReturn(2L);
        when(center.getName()).thenReturn("Owlexa Thủ Đức");
        when(users.searchForAdmin(eq("Nam"), eq(RoleName.TEACHER), any()))
                .thenReturn(new PageImpl<>(List.of(teacher), PageRequest.of(0, 20), 1));

        var response = service(users, centers)
                .getUsers("  Nam  ", RoleName.TEACHER, 0, 20);

        assertThat(response.content()).containsExactly(new AdminUserResponse(
                8L,
                "Nguyễn Văn Nam",
                "0905555551",
                "teacher@owlexa.vn",
                "TEACHER",
                2L,
                "Owlexa Thủ Đức",
                true
        ));
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void searchesAndMapsCentersForAdmin() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        Center center = mock(Center.class);
        User owner = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 8, 12, 0);
        when(center.getId()).thenReturn(3L);
        when(center.getName()).thenReturn("Owlexa Cần Thơ");
        when(center.getSubdomain()).thenReturn("owlexa-cantho");
        when(center.getOwner()).thenReturn(owner);
        when(center.getCreatedAt()).thenReturn(createdAt);
        when(center.isActive()).thenReturn(true);
        when(owner.getId()).thenReturn(30L);
        when(owner.getFullName()).thenReturn("Nguyễn Văn S");
        when(owner.getPhoneNumber()).thenReturn("0901111112");
        when(users.countByCenter_Id(3L)).thenReturn(12L);
        when(centers.searchForAdmin(eq("Cần Thơ"), any()))
                .thenReturn(new PageImpl<>(List.of(center), PageRequest.of(0, 20), 1));

        var response = service(users, centers)
                .getCenters(" Cần Thơ ", 0, 20);

        assertThat(response.content()).containsExactly(new AdminCenterResponse(
                3L,
                "Owlexa Cần Thơ",
                "owlexa-cantho",
                30L,
                "Nguyễn Văn S",
                "0901111112",
                12L,
                createdAt,
                true
        ));
    }

    @Test
    void updatesRegularUserStatus() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        AdminAuditLogRepository auditLogs = mock(AdminAuditLogRepository.class);
        User teacher = mock(User.class);
        User admin = mock(User.class);
        when(users.findById(8L)).thenReturn(java.util.Optional.of(teacher));
        when(users.findByPhoneNumber("0900000000")).thenReturn(java.util.Optional.of(admin));
        when(teacher.getRole()).thenReturn(RoleName.TEACHER);
        when(teacher.getId()).thenReturn(8L);
        when(teacher.getFullName()).thenReturn("Nguyễn Văn Nam");
        when(teacher.getPhoneNumber()).thenReturn("0905555551");
        when(teacher.isActive()).thenReturn(true);
        when(admin.getRole()).thenReturn(RoleName.ADMIN);

        new AdminService(users, centers, auditLogs)
                .updateUserStatus(8L, false, "Vi phạm chính sách", "0900000000");

        verify(teacher).setActive(false);
        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogs).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AdminAuditAction.USER_LOCKED);
        assertThat(logCaptor.getValue().getReason()).isEqualTo("Vi phạm chính sách");
    }

    @Test
    void neverAllowsAdminAccountToBeLocked() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        AdminAuditLogRepository auditLogs = mock(AdminAuditLogRepository.class);
        User admin = mock(User.class);
        when(users.findById(31L)).thenReturn(java.util.Optional.of(admin));
        when(admin.getRole()).thenReturn(RoleName.ADMIN);

        assertThatThrownBy(() -> new AdminService(users, centers, auditLogs)
                .updateUserStatus(31L, false, "Kiểm thử", "0900000000"))
                .isInstanceOf(com.owlexa.owlexabackend.exception.InvalidAdminOperationException.class)
                .hasMessage("Không thể khóa tài khoản quản trị hệ thống");
        verify(admin, never()).setActive(false);
    }

    @Test
    void updatesCenterStatus() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        AdminAuditLogRepository auditLogs = mock(AdminAuditLogRepository.class);
        Center center = mock(Center.class);
        User owner = mock(User.class);
        User admin = mock(User.class);
        when(centers.findById(3L)).thenReturn(java.util.Optional.of(center));
        when(users.findByPhoneNumber("0900000000")).thenReturn(java.util.Optional.of(admin));
        when(center.getId()).thenReturn(3L);
        when(center.getName()).thenReturn("Owlexa Cần Thơ");
        when(center.getOwner()).thenReturn(owner);
        when(center.isActive()).thenReturn(true);
        when(admin.getRole()).thenReturn(RoleName.ADMIN);

        new AdminService(users, centers, auditLogs)
                .updateCenterStatus(3L, false, "Tạm dừng vận hành", "0900000000");

        verify(center).setActive(false);
        verify(auditLogs).save(any(AdminAuditLog.class));
    }

    @Test
    void doesNotWriteAuditLogForStatusNoOp() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        AdminAuditLogRepository auditLogs = mock(AdminAuditLogRepository.class);
        User teacher = mock(User.class);
        when(users.findById(8L)).thenReturn(java.util.Optional.of(teacher));
        when(teacher.getRole()).thenReturn(RoleName.TEACHER);
        when(teacher.isActive()).thenReturn(true);
        when(teacher.getId()).thenReturn(8L);
        when(teacher.getPhoneNumber()).thenReturn("0905555551");

        new AdminService(users, centers, auditLogs)
                .updateUserStatus(8L, true, "Không thay đổi", "0900000000");

        verify(auditLogs, never()).save(any());
    }

    @Test
    void searchesAndMapsAdminAuditLogs() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        AdminAuditLogRepository auditLogs = mock(AdminAuditLogRepository.class);
        AdminAuditLog log = mock(AdminAuditLog.class);
        User admin = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 11, 10, 30);
        when(log.getId()).thenReturn(5L);
        when(log.getAdmin()).thenReturn(admin);
        when(log.getAction()).thenReturn(AdminAuditAction.USER_LOCKED);
        when(log.getTargetType()).thenReturn(AdminAuditTargetType.USER);
        when(log.getTargetId()).thenReturn(8L);
        when(log.getTargetName()).thenReturn("Nguyễn Văn Nam");
        when(log.getPreviousStatus()).thenReturn("ACTIVE");
        when(log.getNewStatus()).thenReturn("INACTIVE");
        when(log.getReason()).thenReturn("Vi phạm chính sách");
        when(log.getCreatedAt()).thenReturn(createdAt);
        when(admin.getId()).thenReturn(31L);
        when(admin.getFullName()).thenReturn("System Administrator");
        when(admin.getPhoneNumber()).thenReturn("0900000000");
        when(auditLogs.searchForAdmin(
                eq("Nam"), eq(AdminAuditTargetType.USER),
                eq(AdminAuditAction.USER_LOCKED), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        var response = new AdminService(users, centers, auditLogs).getAuditLogs(
                " Nam ", AdminAuditTargetType.USER, AdminAuditAction.USER_LOCKED, 0, 20);

        assertThat(response.content()).containsExactly(new AdminAuditLogResponse(
                5L,
                31L,
                "System Administrator",
                "0900000000",
                "USER_LOCKED",
                "USER",
                8L,
                "Nguyễn Văn Nam",
                "ACTIVE",
                "INACTIVE",
                "Vi phạm chính sách",
                createdAt
        ));
    }

    private AdminService service(UserRepository users, CenterRepository centers) {
        return new AdminService(users, centers, mock(AdminAuditLogRepository.class));
    }
}
