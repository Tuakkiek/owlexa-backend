package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.PermissionOverrideType;
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

    private UserPermission up(Permission p, PermissionOverrideType type) {
        UserPermission up = new UserPermission();
        up.setPermission(p);
        up.setType(type);
        return up;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROLE DEFAULTS ONLY
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Role defaults only — no overrides")
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
    // ALLOW OVERRIDE
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ALLOW override adds a permission not in role defaults")
    void resolvePermissions_allowOverride_addsPermission() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission paymentView = perm(4L, "PAYMENT_VIEW");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(up(paymentView, PermissionOverrideType.ALLOW)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactlyInAnyOrder("CLASS_VIEW", "PAYMENT_VIEW");
    }

    @Test
    @DisplayName("ALLOW override on already-granted permission — no duplicate")
    void resolvePermissions_allowOverride_noDuplicate() {
        Permission classView = perm(1L, "CLASS_VIEW");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(up(classView, PermissionOverrideType.ALLOW)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactly("CLASS_VIEW");
    }

    // ═══════════════════════════════════════════════════════════════
    // DENY OVERRIDE
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DENY override removes a role-default permission")
    void resolvePermissions_denyOverride_removesPermission() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission documentUpload = perm(5L, "DOCUMENT_UPLOAD");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView), rp(Role.TEACHER, documentUpload)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(up(documentUpload, PermissionOverrideType.DENY)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactly("CLASS_VIEW");
        assertThat(result).doesNotContain("DOCUMENT_UPLOAD");
    }

    // ═══════════════════════════════════════════════════════════════
    // ALLOW + DENY COMBINED
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ALLOW + DENY combined — DENY wins over ALLOW for the same permission")
    void resolvePermissions_allowAndDeny_denyWins() {
        Permission paymentView = perm(4L, "PAYMENT_VIEW");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of());
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(
                        up(paymentView, PermissionOverrideType.ALLOW),
                        up(paymentView, PermissionOverrideType.DENY)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        // DENY is applied after ALLOW, so it should be removed
        assertThat(result).doesNotContain("PAYMENT_VIEW");
    }

    @Test
    @DisplayName("ALLOW new + DENY existing role default — both applied correctly")
    void resolvePermissions_allowNew_denyExisting() {
        Permission classView = perm(1L, "CLASS_VIEW");
        Permission documentUpload = perm(5L, "DOCUMENT_UPLOAD");
        Permission paymentView = perm(4L, "PAYMENT_VIEW");

        when(rolePermissionRepository.findAllByRole(Role.TEACHER))
                .thenReturn(List.of(rp(Role.TEACHER, classView), rp(Role.TEACHER, documentUpload)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(
                        up(paymentView, PermissionOverrideType.ALLOW),
                        up(documentUpload, PermissionOverrideType.DENY)));

        Set<String> result = resolver.resolvePermissions(USER_ID, Role.TEACHER);

        assertThat(result).containsExactlyInAnyOrder("CLASS_VIEW", "PAYMENT_VIEW");
        assertThat(result).doesNotContain("DOCUMENT_UPLOAD");
    }

    // ═══════════════════════════════════════════════════════════════
    // CACHE EVICTION
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("evictCache does not throw — annotation-based, body is empty")
    void evictCache_doesNotThrow() {
        // The evictCache method is intentionally empty — @CacheEvict handles the work.
        // This test verifies it exists and can be called without error.
        resolver.evictCache(1L);
        resolver.evictCache(null); // should also not throw
    }
}
