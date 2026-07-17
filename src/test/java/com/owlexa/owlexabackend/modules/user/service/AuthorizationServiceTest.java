package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PermissionResolver permissionResolver;
    @Mock private CenterRepository centerRepository;

    private AuthorizationService service;

    private static final String PHONE = "0901234567";
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(userRepository, permissionResolver, centerRepository);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PHONE, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Role role) {
        User user = new User();
        user.setId(USER_ID);
        user.setPhoneNumber(PHONE);
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("hasRole: role khớp → true")
    void hasRole_whenRoleMatches_shouldReturnTrue() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));

        assertThat(service.hasRole(Role.OWNER)).isTrue();
    }

    @Test
    @DisplayName("hasRole: role không khớp → false")
    void hasRole_whenRoleDoesNotMatch_shouldReturnFalse() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.STUDENT)));

        assertThat(service.hasRole(Role.OWNER)).isFalse();
    }

    @Test
    @DisplayName("hasPermission: code null → false (không throw)")
    void hasPermission_whenCodeIsNull_shouldReturnFalse() {
        assertThat(service.hasPermission(null)).isFalse();
    }

    @Test
    @DisplayName("hasPermission: code blank → false")
    void hasPermission_whenCodeIsBlank_shouldReturnFalse() {
        assertThat(service.hasPermission("   ")).isFalse();
    }

    @Test
    @DisplayName("hasPermission: code lowercase → normalize uppercase → true")
    void hasPermission_shouldNormalizeCodeToUppercase() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(permissionResolver.resolvePermissions(USER_ID, Role.OWNER))
                .thenReturn(Set.of("CENTER_CREATE"));

        assertThat(service.hasPermission("center_create")).isTrue();
    }

    @Test
    @DisplayName("hasPermission: user có permission → true")
    void hasPermission_whenUserHasPermission_shouldReturnTrue() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(permissionResolver.resolvePermissions(USER_ID, Role.OWNER))
                .thenReturn(Set.of("FEE_COLLECT"));

        assertThat(service.hasPermission("FEE_COLLECT")).isTrue();
    }

    @Test
    @DisplayName("hasPermission: user không có permission → false")
    void hasPermission_whenUserDoesNotHavePermission_shouldReturnFalse() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(permissionResolver.resolvePermissions(USER_ID, Role.OWNER))
                .thenReturn(Set.of("OTHER_PERMISSION"));

        assertThat(service.hasPermission("FEE_COLLECT")).isFalse();
    }

    @Test
    @DisplayName("isOwnerOfCenter: centerId null → false")
    void isOwnerOfCenter_whenCenterIdIsNull_shouldReturnFalse() {
        assertThat(service.isOwnerOfCenter(null)).isFalse();
    }

    @Test
    @DisplayName("isOwnerOfCenter: user là owner của center → true")
    void isOwnerOfCenter_whenUserIsOwner_shouldReturnTrue() {
        User owner = buildUser(Role.OWNER);
        Center center = new Center();
        center.setId(1L);
        center.setOwner(owner);

        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(owner));
        when(centerRepository.findById(1L)).thenReturn(Optional.of(center));

        assertThat(service.isOwnerOfCenter(1L)).isTrue();
    }

    @Test
    @DisplayName("isOwnerOfCenter: user không phải owner → false")
    void isOwnerOfCenter_whenUserIsNotOwner_shouldReturnFalse() {
        User current = buildUser(Role.OWNER);
        User otherOwner = buildUser(Role.OWNER);
        otherOwner.setId(99L);

        Center center = new Center();
        center.setId(1L);
        center.setOwner(otherOwner);

        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(current));
        when(centerRepository.findById(1L)).thenReturn(Optional.of(center));

        assertThat(service.isOwnerOfCenter(1L)).isFalse();
    }

    @Test
    @DisplayName("isOwnerOfCenter: center không tồn tại → false")
    void isOwnerOfCenter_whenCenterNotFound_shouldReturnFalse() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(centerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(service.isOwnerOfCenter(404L)).isFalse();
    }

    @Test
    @DisplayName("getCurrentUser: chưa authenticate → AccessDeniedException")
    void hasRole_whenNotAuthenticated_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.hasRole(Role.OWNER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getCurrentUser: user không tồn tại trong DB → ResourceNotFoundException")
    void hasRole_whenUserNotFoundInDb_shouldThrowResourceNotFound() {
        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.hasRole(Role.OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}