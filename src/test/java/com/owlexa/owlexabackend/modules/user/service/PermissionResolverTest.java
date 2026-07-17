package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.user.repository.RolePermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionResolverTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    private PermissionResolver resolver;

    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        resolver = new PermissionResolver(rolePermissionRepository, userPermissionRepository);
    }

    // ── helpers ──

    private Permission perm(Long id, String code) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setDescription("Description for " + code);
        return p;
    }

    private RolePermission rp(Role role, Permission p) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setPermission(p);
        return rp;
    }

    /** Creates a user_permission row — presence means "disabled" in the simplified model. */
    private UserPermission disabled(Permission p) {
        UserPermission up = new UserPermission();
        up.setPermission(p);
        return up;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROLE DEFAULTS ONLY (no disabled permissions)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("All role defaults returned when no permissions are disabled")
    void resolvePermissions_roleDefaultsOnly() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission scheduleView = perm(2L, "SCHEDULE_VIEW");
        Permission attendanceMark = perm(3L, "ATTENDANCE_MARK");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView), rp(Role.TEACHER, scheduleView), rp(Role.TEACHER, attendanceMark)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactlyInAnyOrder("CLASS_VIEW", "SCHEDULE_VIEW", "ATTENDANCE_MARK");
    }

    @Test
    @DisplayName("Null role — returns empty set")
    void resolvePermissions_nullRole_returnsEmpty() {
        when(userPermissionRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        Set<String> result = resolver.resolvePermissions(USER_ID, null);

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    // DISABLED PERMISSIONS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Disabled permission is removed from role defaults")
    void resolvePermissions_disabled_removed() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission documentUpload = perm(5L, "DOCUMENT_UPLOAD");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView), rp(Role.TEACHER, documentUpload)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(disabled(documentUpload)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactly("CLASS_VIEW");
        assertThat(result).doesNotContain("DOCUMENT_UPLOAD");
    }

    @Test
    @DisplayName("Multiple disabled permissions are all removed")
    void resolvePermissions_multipleDisabled_removed() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission scheduleView = perm(2L, "SCHEDULE_VIEW");
        Permission attendanceMark = perm(3L, "ATTENDANCE_MARK");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(
                        rp(Role.TEACHER, classView),
                        rp(Role.TEACHER, scheduleView),
                        rp(Role.TEACHER, attendanceMark)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(disabled(scheduleView), disabled(attendanceMark)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactly("CLASS_VIEW");
    }

    @Test
    @DisplayName("Disabled permission NOT in role is ignored (harmless no-op)")
    void resolvePermissions_disabledNotInRole_ignored() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission paymentView = perm(4L, "PAYMENT_VIEW"); // not in TEACHER role

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(disabled(paymentView)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        // PAYMENT_VIEW was never in the set, so "removing" it is a no-op
        assertThat(result).containsExactly("CLASS_VIEW");
    }

    @Test
    @DisplayName("All role permissions disabled — returns empty set")
    void resolvePermissions_allDisabled_returnsEmpty() {
        Permission classView = perm(1L, "CLASS_VIEW");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(disabled(classView)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════
    // CACHE EVICTION
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("evictCache does not throw — annotation-based, body is empty")
    void evictCache_doesNotThrow() {
        resolver.evictCache(1L);
        resolver.evictCache(null);
    }
}
