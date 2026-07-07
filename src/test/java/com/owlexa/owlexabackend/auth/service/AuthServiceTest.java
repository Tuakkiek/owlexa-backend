package com.owlexa.owlexabackend.auth.service;
import com.owlexa.owlexabackend.modules.auth.service.AuthService;
import com.owlexa.owlexabackend.modules.auth.dto.request.LoginRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RefreshTokenRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.modules.auth.dto.response.AuthResponse;
import com.owlexa.owlexabackend.modules.auth.dto.response.SessionResponse;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import com.owlexa.owlexabackend.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private AuthService authService;

    private HttpServletRequest createMockRequest() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Windows");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        return request;
    }

    @Test
    void registerStudent_shouldCreateStudentAccount() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setPhoneNumber("0901234567");
        request.setEmail("student@example.com");
        request.setFullName("Nguyen Van A");
        request.setPassword("123456");

        HttpServletRequest httpRequest = createMockRequest();

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(jwtUtil.generateRefreshToken(eq("0901234567"), anyString())).thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken(eq("0901234567"), eq("STUDENT"), anyString())).thenReturn("access-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("hashed-refresh-token");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        lenient().when(membershipRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        AuthService.LoginResult result = authService.registerStudent(request, httpRequest);
        AuthResponse response = result.getAuthResponse();

        assertThat(response.getPhoneNumber()).isEqualTo("0901234567");
        assertThat(response.getEmail()).isEqualTo("student@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getRoleName()).isEqualTo(Role.STUDENT.name());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUser().getId()).isEqualTo(1L);
        assertThat(savedSession.getRefreshTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(savedSession.isActive()).isTrue();
    }

    @Test
    void registerStudent_whenPhoneNumberExists_shouldThrowDuplicateException() {
        RegisterStudentRequest request = new RegisterStudentRequest();
        request.setPhoneNumber("0901234567");
        request.setEmail("student@example.com");
        request.setFullName("Nguyen Van A");
        request.setPassword("123456");

        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);

        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerStudent(request, httpRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("phoneNumber");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerOwner_shouldCreateOwnerAccount() {
        RegisterOwnerRequest request = new RegisterOwnerRequest();
        request.setPhoneNumber("0901234568");
        request.setEmail("owner@example.com");
        request.setFullName("Nguyen Van B");
        request.setPassword("123456");

        HttpServletRequest httpRequest = createMockRequest();

        when(userRepository.existsByPhoneNumber("0901234568")).thenReturn(false);
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(jwtUtil.generateRefreshToken(eq("0901234568"), anyString())).thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken(eq("0901234568"), eq("OWNER"), anyString())).thenReturn("access-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("hashed-refresh-token");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        lenient().when(membershipRepository.findAllByUser_Id(2L)).thenReturn(List.of());

        AuthService.LoginResult result = authService.registerOwner(request, httpRequest);
        AuthResponse response = result.getAuthResponse();

        assertThat(response.getPhoneNumber()).isEqualTo("0901234568");
        assertThat(response.getEmail()).isEqualTo("owner@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(response.getRoleName()).isEqualTo(Role.OWNER.name());
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUser().getId()).isEqualTo(2L);
        assertThat(savedSession.getRefreshTokenHash()).isEqualTo("hashed-refresh-token");
    }

    @Test
    void login_whenPasswordIsInvalid_shouldThrowBadRequestException() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0901234567");
        request.setPassword("wrong-password");

        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setPassword("encoded-password");
        user.setRole(Role.STUDENT);

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password");

        verify(sessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void login_whenPasswordValid_shouldReturnAuthResponseAndSaveSession() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0901234567");
        request.setPassword("123456");
        request.setDeviceName("iPhone 15");
        request.setDeviceType("MOBILE");

        HttpServletRequest httpRequest = createMockRequest();

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setEmail("student@example.com");
        user.setFullName("Nguyen Van A");
        user.setRole(Role.STUDENT);
        user.setPassword("encoded-password");

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded-password"))
                .thenReturn(true);
        when(jwtUtil.generateRefreshToken(eq("0901234567"), anyString()))
                .thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken(eq("0901234567"), eq("STUDENT"), anyString()))
                .thenReturn("access-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("hashed-refresh-token");
        lenient().when(membershipRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        AuthService.LoginResult result = authService.login(request, httpRequest);
        AuthResponse response = result.getAuthResponse();

        assertThat(response.getPhoneNumber()).isEqualTo("0901234567");
        assertThat(response.getEmail()).isEqualTo("student@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getRoleName()).isEqualTo("STUDENT");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUser().getId()).isEqualTo(1L);
        assertThat(savedSession.getRefreshTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(savedSession.getDeviceName()).isEqualTo("iPhone 15");
        assertThat(savedSession.getDeviceType()).isEqualTo(DeviceType.MOBILE);
        assertThat(savedSession.isActive()).isTrue();
    }

    @Test
    void login_whenDeviceNameLooksLikeUserAgent_shouldFallbackToShortDeviceName() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0901234567");
        request.setPassword("123456");
        request.setDeviceName("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        request.setDeviceType("WEB");

        HttpServletRequest httpRequest = createMockRequest();

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setRole(Role.STUDENT);
        user.setPassword("encoded-password");

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded-password"))
                .thenReturn(true);
        when(jwtUtil.generateRefreshToken(eq("0901234567"), anyString()))
                .thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken(eq("0901234567"), eq("STUDENT"), anyString()))
                .thenReturn("access-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("hashed-refresh-token");
        lenient().when(membershipRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        authService.login(request, httpRequest);

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getDeviceName()).isEqualTo("Windows PC");
        assertThat(savedSession.getDeviceType()).isEqualTo(DeviceType.DESKTOP);
        assertThat(savedSession.getUserAgent()).isEqualTo("Mozilla/5.0 Windows");
    }

    @Test
    void login_withLegacyPlaintextPassword_shouldUpgradeToBcrypt() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0901234567");
        request.setPassword("123456");

        HttpServletRequest httpRequest = createMockRequest();

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setRole(Role.STUDENT);
        user.setPassword("123456"); // Plaintext

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "123456"))
                .thenReturn(false); // Bcrypt matches fails
        when(passwordEncoder.encode("123456")).thenReturn("new-encoded-password");

        when(jwtUtil.generateRefreshToken(eq("0901234567"), anyString()))
                .thenReturn("refresh-token");
        when(jwtUtil.generateAccessToken(eq("0901234567"), eq("STUDENT"), anyString()))
                .thenReturn("access-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("hashed-refresh-token");
        lenient().when(membershipRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        authService.login(request, httpRequest);

        verify(userRepository).updatePasswordById(1L, "new-encoded-password");
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
    }

    @Test
    void refreshToken_whenRefreshTokenIsValid_shouldReturnNewAccessToken() {
        String requestToken = "old-refresh-token";

        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");
        user.setRole(Role.STUDENT);

        UserSession session = new UserSession();
        session.setId("session-123");
        session.setUser(user);
        session.setRefreshTokenHash("hashed-old-refresh-token");
        session.setActive(true);
        session.setExpiredAt(LocalDateTime.now().plusDays(1));

        when(jwtUtil.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("old-refresh-token")).thenReturn("session-123");
        when(jwtUtil.extractSubject("old-refresh-token")).thenReturn("0901234567");

        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(jwtUtil.hashToken("old-refresh-token")).thenReturn("hashed-old-refresh-token");

        when(jwtUtil.generateRefreshToken("0901234567", "session-123")).thenReturn("new-refresh-token");
        when(jwtUtil.generateAccessToken("0901234567", "STUDENT", "session-123")).thenReturn("new-access-token");
        when(jwtUtil.hashToken("new-refresh-token")).thenReturn("hashed-new-refresh-token");
        lenient().when(membershipRepository.findAllByUser_Id(1L)).thenReturn(List.of());

        AuthService.RefreshResult result = authService.refreshToken(requestToken);
        AuthResponse response = result.getAuthResponse();

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getNewRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getPhoneNumber()).isEqualTo("0901234567");

        verify(sessionRepository).save(session);
        assertThat(session.getRefreshTokenHash()).isEqualTo("hashed-new-refresh-token");
    }

    @Test
    void refreshToken_whenTokenIsEmpty_shouldThrowBadRequestException() {
        String requestToken = "";

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Refresh token must not be empty");

        verify(jwtUtil, never()).isRefreshToken(any());
    }

    @Test
    void refreshToken_whenTokenIsNull_shouldThrowBadRequestException() {
        String requestToken = null;

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Refresh token must not be empty");

        verify(jwtUtil, never()).isRefreshToken(any());
    }

    @Test
    void refreshToken_whenTokenIsMalformed_shouldThrowBadRequestException() {
        String requestToken = "malformed-token";

        when(jwtUtil.isRefreshToken("malformed-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("malformed-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Malformed token");
    }

    @Test
    void refreshToken_whenTokenIsReuseDetected_shouldDeactivateAllSessionsAndThrowException() {
        String requestToken = "reuse-refresh-token";

        User user = new User();
        user.setId(1L);

        UserSession session = new UserSession();
        session.setId("session-123");
        session.setUser(user);
        session.setRefreshTokenHash("different-hash");
        session.setActive(true);

        when(jwtUtil.isRefreshToken("reuse-refresh-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("reuse-refresh-token")).thenReturn("session-123");
        when(jwtUtil.extractSubject("reuse-refresh-token")).thenReturn("0901234567");
        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(jwtUtil.hashToken("reuse-refresh-token")).thenReturn("incoming-hash");

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Security alert: token reuse detected");

        verify(sessionRepository).deactivateAllByUserId(1L);
    }

    @Test
    void refreshToken_whenSessionIsRevoked_shouldThrowException() {
        String requestToken = "revoked-refresh-token";

        User user = new User();
        user.setId(1L);

        UserSession session = new UserSession();
        session.setId("session-123");
        session.setUser(user);
        session.setRefreshTokenHash("hashed-token");
        session.setActive(false);

        when(jwtUtil.isRefreshToken("revoked-refresh-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("revoked-refresh-token")).thenReturn("session-123");
        when(jwtUtil.extractSubject("revoked-refresh-token")).thenReturn("0901234567");
        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(jwtUtil.hashToken("revoked-refresh-token")).thenReturn("hashed-token");

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session has been revoked");
    }

    @Test
    void refreshToken_whenSessionIsExpired_shouldDeactivateSessionAndThrowException() {
        String requestToken = "expired-refresh-token";

        User user = new User();
        user.setId(1L);

        UserSession session = new UserSession();
        session.setId("session-123");
        session.setUser(user);
        session.setRefreshTokenHash("hashed-token");
        session.setActive(true);
        session.setExpiredAt(LocalDateTime.now().minusDays(1));

        when(jwtUtil.isRefreshToken("expired-refresh-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("expired-refresh-token")).thenReturn("session-123");
        when(jwtUtil.extractSubject("expired-refresh-token")).thenReturn("0901234567");
        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));
        when(jwtUtil.hashToken("expired-refresh-token")).thenReturn("hashed-token");

        assertThatThrownBy(() -> authService.refreshToken(requestToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session has expired");

        assertThat(session.isActive()).isFalse();
        verify(sessionRepository).save(session);
    }

    @Test
    void logout_shouldDeactivateSession() {
        UserSession session = new UserSession();
        session.setId("session-123");
        session.setActive(true);

        when(sessionRepository.findById("session-123")).thenReturn(Optional.of(session));

        authService.logout("session-123");

        assertThat(session.isActive()).isFalse();
        verify(sessionRepository).save(session);
    }

    @Test
    void logout_whenSessionIdIsNull_shouldThrowException() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session not identified");
    }

    @Test
    void revokeSession_shouldDeactivateSpecificSession() {
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");

        UserSession session = new UserSession();
        session.setId("session-123");
        session.setUser(user);
        session.setActive(true);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUser_Id("session-123", 1L)).thenReturn(Optional.of(session));

        authService.revokeSession("0901234567", "session-123");

        assertThat(session.isActive()).isFalse();
        verify(sessionRepository).save(session);
    }

    @Test
    void revokeAllSessions_shouldDeactivateAllUserSessions() {
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));

        authService.revokeAllSessions("0901234567");

        verify(sessionRepository).deactivateAllByUserId(1L);
    }

    @Test
    void getSessions_shouldReturnActiveSessions() {
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("0901234567");

        UserSession s1 = UserSession.builder()
                .id("session-1")
                .deviceName("Windows PC")
                .deviceType(DeviceType.DESKTOP)
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();

        UserSession s2 = UserSession.builder()
                .id("session-2")
                .deviceName("iPhone")
                .deviceType(DeviceType.MOBILE)
                .ipAddress("10.0.0.1")
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUser_IdAndActiveTrueOrderByLastUsedAtDesc(1L))
                .thenReturn(List.of(s1, s2));

        List<SessionResponse> responses = authService.getSessions("0901234567", "session-2");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSessionId()).isEqualTo("session-1");
        assertThat(responses.get(0).isCurrent()).isFalse();
        assertThat(responses.get(1).getSessionId()).isEqualTo("session-2");
        assertThat(responses.get(1).isCurrent()).isTrue();
    }

    @Test
    void grantPermissionToUser_shouldAddPermission_whenNotAlreadyGranted() {
        User user = new User();
        user.setId(1L);

        Permission permission = new Permission();
        permission.setId(10L);
        permission.setCode("CENTER_CREATE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findByCode("CENTER_CREATE")).thenReturn(Optional.of(permission));

        authService.grantPermissionToUser(1L, "CENTER_CREATE");

        verify(userRepository).save(user);
        assertThat(user.getUserPermissions()).hasSize(1);
        assertThat(user.getUserPermissions().iterator().next().getPermission().getCode()).isEqualTo("CENTER_CREATE");
    }
}
