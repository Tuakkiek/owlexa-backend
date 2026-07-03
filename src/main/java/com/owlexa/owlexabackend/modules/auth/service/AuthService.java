package com.owlexa.owlexabackend.modules.auth.service;
import com.owlexa.owlexabackend.modules.auth.dto.request.LoginRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RefreshTokenRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RegisterOwnerRequest;
import com.owlexa.owlexabackend.modules.auth.dto.request.RegisterStudentRequest;
import com.owlexa.owlexabackend.modules.auth.dto.response.AuthResponse;
import com.owlexa.owlexabackend.modules.auth.dto.response.SessionResponse;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import com.owlexa.owlexabackend.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final UserSessionRepository  sessionRepository;
    private final MembershipRepository   membershipRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;
    private final PermissionRepository   permissionRepository;

    // ═══════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String phone = normalizePhone(request.getPhoneNumber());

        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new BadRequestException("User not found"));

        verifyPassword(request.getPassword(), user);

        // Upgrade mật khẩu plaintext cũ sang bcrypt ngay khi đăng nhập thành công
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String encoded = passwordEncoder.encode(request.getPassword());
            userRepository.updatePasswordById(user.getId(), encoded);
            user.setPassword(encoded); // keep in-memory state consistent
        }

        String role      = user.getRole() != null ? user.getRole().name() : null;
        String sessionId = UUID.randomUUID().toString();

        String refreshToken = jwtUtil.generateRefreshToken(phone, sessionId);
        String accessToken  = jwtUtil.generateAccessToken(phone, role, sessionId);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .center(resolveCenter(user))
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceName(resolveDeviceName(request.getDeviceName(), httpRequest))
                .deviceType(resolveDeviceType(request.getDeviceType(), httpRequest))
                .ipAddress(extractIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();

        sessionRepository.save(session);

        AuthResponse authResponse = buildAuthResponse(accessToken, sessionId, user, role);
        return LoginResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // REFRESH TOKEN (với rotation + reuse detection)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public RefreshResult refreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Refresh token must not be empty");
        }

        if (!jwtUtil.isRefreshToken(token)) {
            throw new BadRequestException("Invalid token type");
        }

        String sessionId   = jwtUtil.extractSessionId(token);
        String phoneNumber = jwtUtil.extractSubject(token);

        if (sessionId == null || phoneNumber == null) {
            throw new BadRequestException("Malformed token");
        }

        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found or already revoked"));

        String incomingHash = jwtUtil.hashToken(token);

        // ── Reuse detection ──────────────────────────────────────────────
        // Nếu hash không khớp → đây là refresh token đã bị rotate trước đó.
        // Kẻ tấn công đã có token cũ, nên chúng ta revoke TOÀN BỘ session.
        if (!incomingHash.equals(session.getRefreshTokenHash())) {
            log.warn("Refresh token reuse detected for userId={} sessionId={}. Revoking all sessions.",
                    session.getUser().getId(), sessionId);
            sessionRepository.deactivateAllByUserId(session.getUser().getId());
            throw new BadRequestException(
                    "Security alert: token reuse detected. All sessions have been revoked. Please login again.");
        }

        if (!session.isActive()) {
            throw new BadRequestException("Session has been revoked. Please login again.");
        }

        if (session.getExpiredAt().isBefore(LocalDateTime.now())) {
            session.setActive(false);
            sessionRepository.save(session);
            throw new BadRequestException("Session has expired. Please login again.");
        }

        // ── Rotation ─────────────────────────────────────────────────────
        // SessionId giữ nguyên, chỉ refresh token thay đổi.
        User   user           = session.getUser();
        String role           = user.getRole() != null ? user.getRole().name() : null;
        String newRefreshToken = jwtUtil.generateRefreshToken(phoneNumber, sessionId);
        String newAccessToken  = jwtUtil.generateAccessToken(phoneNumber, role, sessionId);

        session.setRefreshTokenHash(jwtUtil.hashToken(newRefreshToken));
        session.setLastUsedAt(LocalDateTime.now());
        sessionRepository.save(session);

        AuthResponse authResponse = buildAuthResponse(newAccessToken, sessionId, user, role);
        return RefreshResult.builder()
                .authResponse(authResponse)
                .newRefreshTokenGenerated(true)
                .newRefreshToken(newRefreshToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void logout(String sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("Session not identified");
        }
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setActive(false);
            sessionRepository.save(s);
        });
    }

    /** Revoke một session cụ thể — chỉ cho phép user revoke session của chính mình. */
    @Transactional
    public void revokeSession(String phoneNumber, String sessionId) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSession session = sessionRepository.findByIdAndUser_Id(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setActive(false);
        sessionRepository.save(session);
    }

    /** Revoke tất cả session — "Đăng xuất tất cả thiết bị". */
    @Transactional
    public void revokeAllSessions(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        sessionRepository.deactivateAllByUserId(user.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION LIST
    // ═══════════════════════════════════════════════════════════════

    public List<SessionResponse> getSessions(String phoneNumber, String currentSessionId) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return sessionRepository
                .findByUser_IdAndActiveTrueOrderByLastUsedAtDesc(user.getId())
                .stream()
                .map(s -> SessionResponse.builder()
                        .sessionId(s.getId())
                        .deviceName(s.getDeviceName())
                        .deviceType(s.getDeviceType())
                        .ipAddress(s.getIpAddress())
                        .createdAt(s.getCreatedAt())
                        .lastUsedAt(s.getLastUsedAt())
                        .current(s.getId().equals(currentSessionId))
                        .build())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // REGISTER
    // ═══════════════════════════════════════════════════════════════

    public LoginResult registerStudent(RegisterStudentRequest request, HttpServletRequest httpRequest) {
        return registerUser(request.getPhoneNumber(), request.getEmail(),
                request.getFullName(), request.getPassword(), Role.STUDENT, httpRequest);
    }

    public LoginResult registerOwner(RegisterOwnerRequest request, HttpServletRequest httpRequest) {
        return registerUser(request.getPhoneNumber(), request.getEmail(),
                request.getFullName(), request.getPassword(), Role.OWNER, httpRequest);
    }

    @Transactional
    public LoginResult registerUser(String phoneNumber, String email, String fullName,
                                     String rawPassword, Role role, HttpServletRequest httpRequest) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("phoneNumber is already exists");
        }

        String normalizedEmail = normalizeOptionalEmail(email);
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setEmail(normalizedEmail);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);

        String sessionId    = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber, sessionId);
        String accessToken  = jwtUtil.generateAccessToken(phoneNumber, role.name(), sessionId);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .center(resolveCenter(user))
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceName(resolveDeviceName(null, httpRequest))
                .deviceType(resolveDeviceType(null, httpRequest))
                .ipAddress(extractIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();

        sessionRepository.save(session);

        AuthResponse authResponse = buildAuthResponse(accessToken, sessionId, user, role.name());
        return LoginResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // PERMISSIONS (giữ nguyên logic cũ)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void grantPermissionToUser(Long userId, String permissionCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionCode));

        boolean alreadyGranted = user.getUserPermissions().stream()
                .anyMatch(link -> link.getPermission() != null
                        && link.getPermission().getCode() != null
                        && link.getPermission().getCode().equalsIgnoreCase(permissionCode));

        if (!alreadyGranted) {
            user.grantPermission(permission);
            userRepository.save(user);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Center resolveCenter(User user) {
        return membershipRepository.findAllByUser_Id(user.getId())
                .stream()
                .findFirst()
                .map(Membership::getCenter)
                .orElse(null);
    }

    private void verifyPassword(String rawPassword, User user) {
        boolean bcryptMatch    = passwordEncoder.matches(rawPassword, user.getPassword());
        boolean legacyMatch    = rawPassword != null && rawPassword.equals(user.getPassword());
        if (!bcryptMatch && !legacyMatch) {
            throw new BadRequestException("Invalid password");
        }
    }

    private AuthResponse buildAuthResponse(String accessToken,
                                           String sessionId, User user, String role) {
        String centerName = membershipRepository.findAllByUser_Id(user.getId())
                .stream()
                .findFirst()
                .map(m -> m.getCenter().getName())
                .orElse(null);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .sessionId(sessionId)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(role)
                .centerName(centerName)
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone number must not be empty");
        }
        return phone.trim();
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Ưu tiên tên client khai báo; nếu không có thì suy ra từ User-Agent. */
    private String resolveDeviceName(String clientDeviceName, HttpServletRequest request) {
        String normalizedClientDeviceName = normalizeDeviceName(clientDeviceName);
        if (normalizedClientDeviceName != null && !looksLikeUserAgent(normalizedClientDeviceName)) {
            return normalizedClientDeviceName;
        }
        return deriveDeviceName(request.getHeader("User-Agent"));
    }

    /** Ưu tiên loại client khai báo; nếu không có thì suy ra từ User-Agent. */
    private DeviceType resolveDeviceType(String clientDeviceType, HttpServletRequest request) {
        if (clientDeviceType != null && !clientDeviceType.isBlank()) {
            switch (clientDeviceType.trim().toUpperCase()) {
                case "DESKTOP":
                case "WEB":
                    return DeviceType.DESKTOP;
                case "MOBILE":
                    return DeviceType.MOBILE;
                case "TABLET":
                    return DeviceType.TABLET;
                case "UNKNOWN":
                    return DeviceType.UNKNOWN;
                default:
                    break;
            }
        }
        return deriveDeviceType(request.getHeader("User-Agent"));
    }

    /** Lấy IP thực của client, có tính đến proxy/load balancer. */
    private String normalizeDeviceName(String deviceName) {
        if (deviceName == null) {
            return null;
        }
        String trimmed = deviceName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean looksLikeUserAgent(String value) {
        String lower = value.toLowerCase();
        return lower.contains("mozilla/")
                || lower.contains("applewebkit")
                || lower.contains("chrome/")
                || lower.contains("safari/")
                || lower.contains("gecko/")
                || lower.contains("version/")
                || value.length() > 120;
    }

    private String deriveDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone")) {
            return "iPhone";
        }
        if (ua.contains("ipad")) {
            return "iPad";
        }
        if (ua.contains("android")) {
            if (ua.contains("mobile")) {
                return "Android Phone";
            }
            if (ua.contains("tablet")) {
                return "Android Tablet";
            }
            return "Android Device";
        }
        if (ua.contains("macintosh") || ua.contains("mac os x")) {
            return "Mac";
        }
        if (ua.contains("windows")) {
            return "Windows PC";
        }
        if (ua.contains("linux")) {
            return "Linux PC";
        }
        return "Unknown Device";
    }

    private DeviceType deriveDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || (ua.contains("android") && ua.contains("mobile"))) {
            return DeviceType.MOBILE;
        }
        if (ua.contains("ipad") || (ua.contains("android") && !ua.contains("mobile"))) {
            return DeviceType.TABLET;
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return DeviceType.DESKTOP;
        }
        return DeviceType.UNKNOWN;
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private List<String> getDefaultPermissionCode(Role role) {
        if (role == null) return List.of();
        return switch (role) {
            case OWNER   -> List.of("CENTER_CREATE", "VIEW_STUDENT", "EDIT_FEE");
            case CASHIER -> List.of("EDIT_FEE");
            case TEACHER -> List.of("VIEW_STUDENT");
            case STUDENT -> List.of("VIEW_STUDENT");
            case ADMIN   -> List.of("VIEW_SALARY");
        };
    }

    @lombok.Getter
    @lombok.Builder
    public static class LoginResult {
        private final AuthResponse authResponse;
        private final String refreshToken;
    }

    @lombok.Getter
    @lombok.Builder
    public static class RefreshResult {
        private final AuthResponse authResponse;
        private final boolean newRefreshTokenGenerated;
        private final String newRefreshToken;
        private final String refreshToken;
    }
}
