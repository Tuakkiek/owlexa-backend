package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.PermissionOverrideItem;
import com.owlexa.owlexabackend.modules.user.dto.response.EffectivePermission;
import com.owlexa.owlexabackend.modules.user.dto.response.PermissionResponse;
import com.owlexa.owlexabackend.modules.user.dto.response.UserPermissionsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.RolePermission;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.RolePermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private UserPermissionRepository userPermissionRepository;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private PermissionResolver permissionResolver;

    private UserPermissionService service;

    private static final Long USER_ID = 10L;
    private static final Role USER_ROLE = Role.TEACHER;

    @BeforeEach
    void setUp() {
        service = new UserPermissionService(
                userRepository, permissionRepository,
                rolePermissionRepository, userPermissionRepository,
                sessionRepository, permissionResolver);
    }

    // ── helpers ──

    private User buildUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setRole(USER_ROLE);
        u.setFullName("Test Teacher");
        return u;
    }

    private Permission perm(Long id, String code, String desc) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setDescription(desc);
        return p;
    }

    private RolePermission rp(Role role, Permission p) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setPermission(p);
        return rp;
    }

    private UserPermission up(User user, Permission p) {
        UserPermission up = new UserPermission();
        up.setUser(user);
        up.setPermission(p);
        return up;
    }

    // ═══════════════════════════════════════════════════════════════
    // listAllPermissions
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("listAllPermissions — returns all permissions ordered by code")
    void listAllPermissions_returnsAll() {
        Permission a = perm(1L, "ATTENDANCE_MARK", "Mark attendance");
        Permission b = perm(2L, "CLASS_VIEW", "View classes");
        Permission c = perm(3L, "PAYMENT_VIEW", "View payments");

        when(permissionRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(a, b, c));

        List<PermissionResponse> result = service.listAllPermissions();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCode()).isEqualTo("ATTENDANCE_MARK");
        assertThat(result.get(0).getDescription()).isEqualTo("Mark attendance");
    }

    // ═══════════════════════════════════════════════════════════════
    // getEffectivePermissions
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getEffectivePermissions — role defaults only")
    void getEffectivePermissions_roleDefaultsOnly() {
        Permission classView = perm(1L, "CLASS_VIEW", "View classes");
        Permission essayGrade = perm(2L, "ESSAY_GRADE", "Grade essays");
        User user = buildUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, classView), rp(USER_ROLE, essayGrade)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        UserPermissionsResponse result = service.getEffectivePermissions(USER_ID);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getRoleName()).isEqualTo("TEACHER");
        assertThat(result.getPermissions()).hasSize(2);
        assertThat(result.getPermissions().get(0).getSource()).isEqualTo("ENABLED");
        assertThat(result.getPermissions().get(1).getSource()).isEqualTo("ENABLED");
    }

    @Test
    @DisplayName("getEffectivePermissions — with DENY override shows DISABLED")
    void getEffectivePermissions_withDenyOverride() {
        Permission classView = perm(1L, "CLASS_VIEW", "View classes");
        Permission docUpload = perm(3L, "DOCUMENT_UPLOAD", "Upload docs");
        User user = buildUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, classView), rp(USER_ROLE, docUpload)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(
                        up(user, docUpload)));

        UserPermissionsResponse result = service.getEffectivePermissions(USER_ID);

        assertThat(result.getPermissions()).hasSize(2);
        EffectivePermission cp = result.getPermissions().stream()
                .filter(p -> p.getCode().equals("CLASS_VIEW")).findFirst().orElseThrow();
        assertThat(cp.getSource()).isEqualTo("ENABLED");

        EffectivePermission dp = result.getPermissions().stream()
                .filter(p -> p.getCode().equals("DOCUMENT_UPLOAD")).findFirst().orElseThrow();
        assertThat(dp.getSource()).isEqualTo("DISABLED");
    }

    @Test
    @DisplayName("getEffectivePermissions — user not found throws")
    void getEffectivePermissions_userNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEffectivePermissions(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ═══════════════════════════════════════════════════════════════
    // applyOverrides
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("applyOverrides — DENY disables a role permission")
    void applyOverrides_denyDisablesRolePermission() {
        User user = buildUser();
        Permission attMark = perm(2L, "ATTENDANCE_MARK", "Mark attendance");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(permissionRepository.findByCode("ATTENDANCE_MARK"))
                .thenReturn(Optional.of(attMark));
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, attMark)));
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        BulkPermissionOverrideRequest request = BulkPermissionOverrideRequest.builder()
                .overrides(List.of(PermissionOverrideItem.builder()
                        .permissionCode("ATTENDANCE_MARK")
                        .type("DENY")
                        .build()))
                .build();

        UserPermissionsResponse result = service.applyOverrides(USER_ID, request);

        verify(userPermissionRepository).deleteByUser_Id(USER_ID);
        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        verify(userPermissionRepository).save(captor.capture());
        assertThat(captor.getValue().getPermission().getCode()).isEqualTo("ATTENDANCE_MARK");
        assertThat(captor.getValue().getPermission().getCode()).isEqualTo("ATTENDANCE_MARK");
        verify(permissionResolver).evictCache(USER_ID);
    }

    @Test
    @DisplayName("applyOverrides — empty overrides list = remove all")
    void applyOverrides_emptyList_removesAll() {
        User user = buildUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of());
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        BulkPermissionOverrideRequest request = BulkPermissionOverrideRequest.builder()
                .overrides(List.of())
                .build();

        service.applyOverrides(USER_ID, request);

        verify(userPermissionRepository).deleteByUser_Id(USER_ID);
        verify(userPermissionRepository, never()).save(any());
        verify(permissionResolver).evictCache(USER_ID);
    }

    @Test
    @DisplayName("applyOverrides — INHERIT type is skipped (no save)")
    void applyOverrides_inheritType_skipped() {
        User user = buildUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of());
        when(userPermissionRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of());

        BulkPermissionOverrideRequest request = BulkPermissionOverrideRequest.builder()
                .overrides(List.of(PermissionOverrideItem.builder()
                        .permissionCode("CLASS_VIEW")
                        .type("INHERIT")
                        .build()))
                .build();

        service.applyOverrides(USER_ID, request);

        verify(userPermissionRepository).deleteByUser_Id(USER_ID);
        verify(userPermissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyOverrides — invalid type throws")
    void applyOverrides_invalidType_throws() {
        User user = buildUser();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        BulkPermissionOverrideRequest request = BulkPermissionOverrideRequest.builder()
                .overrides(List.of(PermissionOverrideItem.builder()
                        .permissionCode("CLASS_VIEW")
                        .type("INVALID")
                        .build()))
                .build();

        assertThatThrownBy(() -> service.applyOverrides(USER_ID, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("applyOverrides — non-role permission rejected")
    void applyOverrides_nonRolePermission_rejected() {
        User user = buildUser();
        Permission classView = perm(1L, "CLASS_VIEW", "View classes");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        // TEACHER role does NOT have PAYMENT_VIEW
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, classView)));

        BulkPermissionOverrideRequest request = BulkPermissionOverrideRequest.builder()
                .overrides(List.of(PermissionOverrideItem.builder()
                        .permissionCode("PAYMENT_VIEW")
                        .type("DENY")
                        .build()))
                .build();

        assertThatThrownBy(() -> service.applyOverrides(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không thuộc vai trò");
    }

    // ═══════════════════════════════════════════════════════════════
    // updateSingleOverride
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateSingleOverride — DISABLED creates disable record")
    void updateSingleOverride_disable() {
        User user = buildUser();
        Permission attMark = perm(2L, "ATTENDANCE_MARK", "Mark attendance");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(permissionRepository.findByCode("ATTENDANCE_MARK"))
                .thenReturn(Optional.of(attMark));
        when(userPermissionRepository.findByUser_IdAndPermission_Code(USER_ID, "ATTENDANCE_MARK"))
                .thenReturn(Optional.empty());
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, attMark)));

        EffectivePermission result = service.updateSingleOverride(USER_ID, "ATTENDANCE_MARK", "DISABLED");

        assertThat(result.getSource()).isEqualTo("DISABLED");
        verify(userPermissionRepository).save(any(UserPermission.class));
        verify(permissionResolver).evictCache(USER_ID);
    }

    @Test
    @DisplayName("updateSingleOverride — INHERIT removes existing disable record")
    void updateSingleOverride_inherit() {
        User user = buildUser();
        Permission docUpload = perm(2L, "DOCUMENT_UPLOAD", "Upload docs");
        UserPermission existing = up(user, docUpload);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(permissionRepository.findByCode("DOCUMENT_UPLOAD"))
                .thenReturn(Optional.of(docUpload));
        when(userPermissionRepository.findByUser_IdAndPermission_Code(USER_ID, "DOCUMENT_UPLOAD"))
                .thenReturn(Optional.of(existing));
        when(rolePermissionRepository.findAllByRole(USER_ROLE))
                .thenReturn(List.of(rp(USER_ROLE, docUpload)));

        EffectivePermission result = service.updateSingleOverride(USER_ID, "DOCUMENT_UPLOAD", "INHERIT");

        assertThat(result.getSource()).isEqualTo("ENABLED");
        verify(userPermissionRepository).delete(existing);
        verify(userPermissionRepository, never()).save(any());
        verify(permissionResolver).evictCache(USER_ID);
    }

    // ═══════════════════════════════════════════════════════════════
    // removeAllOverrides
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("removeAllOverrides — deletes all and evicts cache")
    void removeAllOverrides_deletesAndEvicts() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        service.removeAllOverrides(USER_ID);

        verify(userPermissionRepository).deleteByUser_Id(USER_ID);
        verify(permissionResolver).evictCache(USER_ID);
    }

    @Test
    @DisplayName("removeAllOverrides — user not found throws")
    void removeAllOverrides_userNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.removeAllOverrides(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
