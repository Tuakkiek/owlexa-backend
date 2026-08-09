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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;

import java.time.Instant;
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
    private final CenterRepository       centerRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;
    private final PermissionRepository   permissionRepository;
    private final com.owlexa.owlexabackend.modules.user.service.PermissionResolver permissionResolver;

    @Value("${app.session.inactive-timeout-days:30}")
    private int inactiveTimeoutDays;

    @Value("${app.session.absolute-timeout-days:90}")
    private int absoluteTimeoutDays;

    @Value("${app.session.max-devices:5}")
    private int maxDevices;

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

        String role       = user.getRole() != null ? user.getRole().name() : null;
        String userAgent  = httpRequest.getHeader("User-Agent");
        String deviceKey  = jwtUtil.hashDeviceKey(user.getId(), userAgent);
        String deviceName = resolveDeviceName(request.getDeviceName(), httpRequest);
        DeviceType deviceType = resolveDeviceType(request.getDeviceType(), httpRequest);
        String ipAddr     = extractIp(httpRequest);

        // ── Device dedup: same device → reuse, don't create new session ──
        UserSession session = sessionRepository
                .findByUser_IdAndDeviceKeyAndActiveTrue(user.getId(), deviceKey)
                .orElse(null);

        if (session != null) {
            return reuseExistingSession(session, user, role, deviceName, deviceType, ipAddr, userAgent);
        }

        // ── New device: enforce max-devices limit ────────────────────────
        long activeCount = sessionRepository.countByUser_IdAndActiveTrue(user.getId());
        if (activeCount >= maxDevices) {
            sessionRepository.findFirstByUser_IdAndActiveTrueOrderByLastUsedAtAsc(user.getId())
                    .ifPresent(oldest -> {
                        log.info("Evicting oldest session {} (lastUsed={}) for userId={} — device limit {} reached",
                                oldest.getId(), oldest.getLastUsedAt(), user.getId(), maxDevices);
                        sessionRepository.delete(oldest);
                    });
        }

        // ── Create new session ───────────────────────────────────────────
        String sessionId    = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(phone, sessionId);
        String accessToken  = jwtUtil.generateAccessToken(phone, role, sessionId);
        LocalDateTime now   = LocalDateTime.now();

        UserSession newSession = UserSession.builder()
                .id(sessionId)
                .user(user)
                .center(resolveCenter(user))
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceName(deviceName)
                .deviceType(deviceType)
                .deviceKey(deviceKey)
                .ipAddress(ipAddr)
                .userAgent(userAgent)
                .active(true)
                .createdAt(now)
                .lastUsedAt(now)
                .inactiveExpireAt(now.plusDays(inactiveTimeoutDays))
                .absoluteExpireAt(now.plusDays(absoluteTimeoutDays))
                .rotationCount(0)
                .build();

        sessionRepository.save(newSession);

        AuthResponse authResponse = buildAuthResponse(accessToken, sessionId, user, role);
        return LoginResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .build();
    }

    /** Reuse an existing session: rotate tokens, update metadata, extend sliding expiration. */
    private LoginResult reuseExistingSession(UserSession session, User user, String role,
                                              String deviceName, DeviceType deviceType,
                                              String ipAddr, String userAgent) {
        String phone       = user.getPhoneNumber();
        String sessionId   = session.getId();
        String refreshToken = jwtUtil.generateRefreshToken(phone, sessionId);
        String accessToken  = jwtUtil.generateAccessToken(phone, role, sessionId);
        LocalDateTime now   = LocalDateTime.now();

        session.setRefreshTokenHash(jwtUtil.hashToken(refreshToken));
        session.setDeviceName(deviceName);
        session.setDeviceType(deviceType);
        session.setIpAddress(ipAddr);
        session.setUserAgent(userAgent);
        session.setLastUsedAt(now);
        session.setInactiveExpireAt(now.plusDays(inactiveTimeoutDays));
        // absoluteExpireAt is never extended — it stays at the original value
        sessionRepository.save(session);

        AuthResponse authResponse = buildAuthResponse(accessToken, sessionId, user, role);
        return LoginResult.builder()
                .authResponse(authResponse)
                .refreshToken(refreshToken)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // REFRESH TOKEN (rotation + reuse detection + sliding + absolute)
    // ═══════════════════════════════════════════════════════════════

    @Transactional(noRollbackFor = BadRequestException.class)
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
        if (!incomingHash.equals(session.getRefreshTokenHash())) {
            log.warn("Refresh token reuse detected for userId={} sessionId={}. Revoking all sessions.",
                    session.getUser().getId(), sessionId);
            sessionRepository.deactivateAllByUserIdWithReason(
                    session.getUser().getId(), "REUSE_DETECTED");
            throw new BadRequestException(
                    "Security alert: token reuse detected. All sessions have been revoked. Please login again.");
        }

        if (!session.isActive()) {
            throw new BadRequestException("Session has been revoked. Please login again.");
        }

        // ── Absolute expiration check ────────────────────────────────────
        if (session.getAbsoluteExpireAt().isBefore(LocalDateTime.now())) {
            session.setActive(false);
            session.setRevokedReason("ABSOLUTE_EXPIRED");
            session.setRevokedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new BadRequestException("Session has reached its maximum lifetime. Please login again.");
        }

        // ── Sliding expiration check ─────────────────────────────────────
        if (session.getInactiveExpireAt().isBefore(LocalDateTime.now())) {
            session.setActive(false);
            session.setRevokedReason("INACTIVE_EXPIRED");
            session.setRevokedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new BadRequestException("Session has expired due to inactivity. Please login again.");
        }

        // ── Rotation ─────────────────────────────────────────────────────
        User   user            = session.getUser();
        String role            = user.getRole() != null ? user.getRole().name() : null;
        String newRefreshToken = jwtUtil.generateRefreshToken(phoneNumber, sessionId);
        String newAccessToken  = jwtUtil.generateAccessToken(phoneNumber, role, sessionId);
        LocalDateTime now      = LocalDateTime.now();

        session.setRefreshTokenHash(jwtUtil.hashToken(newRefreshToken));
        session.setLastUsedAt(now);
        session.setInactiveExpireAt(now.plusDays(inactiveTimeoutDays)); // sliding extension
        session.setRotationCount(session.getRotationCount() + 1);
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
            s.setRevokedReason("USER_LOGOUT");
            s.setRevokedAt(LocalDateTime.now());
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
        session.setRevokedReason("MANUAL_REVOKE");
        session.setRevokedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /** Revoke tất cả session — "Đăng xuất tất cả thiết bị". */
    @Transactional
    public void revokeAllSessions(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        sessionRepository.deactivateAllByUserIdWithReason(user.getId(), "MANUAL_REVOKE_ALL");
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
        throw new org.springframework.security.access.AccessDeniedException(
                "Học viên không thể tự đăng ký. Chỉ có Chủ trung tâm mới có quyền tạo người dùng."
        );
    }

    @Transactional
    public LoginResult registerOwner(RegisterOwnerRequest request, HttpServletRequest httpRequest) {
        String phoneNumber = normalizePhone(request.getPhoneNumber());
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("phoneNumber is already exists");
        }

        String normalizedEmail = normalizeOptionalEmail(request.getEmail());
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        String centerName = request.getCenterName() != null ? request.getCenterName().trim() : "";
        if (centerName.isBlank()) {
            throw new BadRequestException("Center name is required");
        }

        String rawSub = request.getSubdomain() != null ? request.getSubdomain().trim().toLowerCase() : "";
        String subdomain = rawSub.isEmpty()
                ? centerName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")
                : rawSub.replaceAll("[^a-z0-9-]+", "");
        if (subdomain.isBlank()) {
            subdomain = "center-" + System.currentTimeMillis();
        }

        if (centerRepository.existsBySubdomain(subdomain)) {
            throw new DuplicateResourceException("Subdomain already exists: " + subdomain);
        }

        // 1. Create Owner User
        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.OWNER);
        user = userRepository.save(user);

        // 2. Create Primary Center for Owner
        Center center = new Center();
        center.setName(centerName);
        center.setSubdomain(subdomain);
        center.setOwner(user);
        center = centerRepository.save(center);

        // 3. Create Owner Membership
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCenter(center);
        membership.setJoinedByUser(user);
        membership.setJoinedAt(Instant.now());
        membershipRepository.save(membership);

        // 4. Create Session
        String sessionId    = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber, sessionId);
        String accessToken  = jwtUtil.generateAccessToken(phoneNumber, Role.OWNER.name(), sessionId);
        String userAgent    = httpRequest.getHeader("User-Agent");
        LocalDateTime now   = LocalDateTime.now();

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .center(center)
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceName(resolveDeviceName(null, httpRequest))
                .deviceType(resolveDeviceType(null, httpRequest))
                .deviceKey(jwtUtil.hashDeviceKey(user.getId(), userAgent))
                .ipAddress(extractIp(httpRequest))
                .userAgent(userAgent)
                .active(true)
                .createdAt(now)
                .lastUsedAt(now)
                .inactiveExpireAt(now.plusDays(inactiveTimeoutDays))
                .absoluteExpireAt(now.plusDays(absoluteTimeoutDays))
                .rotationCount(0)
                .build();

        sessionRepository.save(session);

        AuthResponse authResponse = buildAuthResponse(accessToken, sessionId, user, Role.OWNER.name());
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
        Membership firstMembership = membershipRepository.findAllByUser_Id(user.getId())
                .stream()
                .findFirst()
                .orElse(null);
        String centerName = firstMembership != null ? firstMembership.getCenter().getName() : null;
        Long centerId = firstMembership != null ? firstMembership.getCenter().getId() : null;
        
        java.util.List<String> permissions = new java.util.ArrayList<>(
            permissionResolver.resolvePermissions(user.getId(), user.getRole())
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(role)
                .centerName(centerName)
                .centerId(centerId)
                .permissions(permissions)
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
