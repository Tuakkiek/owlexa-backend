package com.owlexa.owlexabackend.integration.auth;

import com.owlexa.owlexabackend.common.security.JwtUtil;
import com.owlexa.owlexabackend.integration.BaseIntegrationTest;
import com.owlexa.owlexabackend.modules.auth.service.AuthService;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for session management behaviors:
 * <ul>
 *   <li>Device deduplication (same UA → reuse session)</li>
 *   <li>Multi-device limit enforcement (max 5, evict oldest)</li>
 *   <li>Refresh token rotation count</li>
 *   <li>Sliding expiration on refresh</li>
 *   <li>Absolute expiration enforcement</li>
 *   <li>Revoked reason tracking</li>
 * </ul>
 */
class SessionManagementIntegrationTest extends BaseIntegrationTest {

    private static final String LOGIN_URL = "/auth/login";
    private static final String REFRESH_URL = "/auth/refresh-token";
    private static final String LOGOUT_URL = "/auth/logout";

    private static final String SEED_PHONE = "0900000100";
    private static final String SEED_PASSWORD = "Secret#123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @AfterEach
    void cleanUp() {
        sessionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        SecurityContextHolder.clearContext();
    }

    // ───────────────────────────────────────────────────────────────────────
    // DEDUP: same device → reuse session
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login twice from same User-Agent reuses session (does not create new one)")
    void loginTwiceSameDevice_shouldReuseSession() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        // First login
        MvcResult result1 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD))
                        .header("User-Agent", "Chrome/120.0 Windows"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("sessionId").asText();
        long count1 = sessionRepository.countByUser_IdAndActiveTrue(getUserId(SEED_PHONE));

        // Second login — same UA
        MvcResult result2 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD))
                        .header("User-Agent", "Chrome/120.0 Windows"))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("sessionId").asText();
        long count2 = sessionRepository.countByUser_IdAndActiveTrue(getUserId(SEED_PHONE));

        // Same session ID reused, count unchanged (still 1)
        assertThat(sessionId1).isEqualTo(sessionId2);
        assertThat(count1).isEqualTo(1);
        assertThat(count2).isEqualTo(1);
    }

    @Test
    @DisplayName("Login from different User-Agents creates separate sessions")
    void loginFromDifferentDevices_shouldCreateSeparateSessions() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        // Chrome desktop
        MvcResult r1 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD))
                        .header("User-Agent", "Mozilla/5.0 Chrome/120.0 Windows"))
                .andExpect(status().isOk())
                .andReturn();
        String sid1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("sessionId").asText();

        // Firefox desktop
        MvcResult r2 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD))
                        .header("User-Agent", "Mozilla/5.0 Firefox/121.0 Windows"))
                .andExpect(status().isOk())
                .andReturn();
        String sid2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("sessionId").asText();

        long count = sessionRepository.countByUser_IdAndActiveTrue(getUserId(SEED_PHONE));

        assertThat(sid1).isNotEqualTo(sid2);
        assertThat(count).isEqualTo(2);
    }

    // ───────────────────────────────────────────────────────────────────────
    // LIMIT: max 5 devices, evict oldest
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("When 5 active sessions exist, login from 6th device evicts oldest and creates new")
    void loginExceedingDeviceLimit_shouldEvictOldest() throws Exception {
        User user = seedOwner(SEED_PHONE, SEED_PASSWORD);
        Long userId = user.getId();

        // Create 5 active sessions from 5 different devices
        String oldestSessionId = null;
        for (int i = 1; i <= 5; i++) {
            String sid = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now().minusHours(6 - i); // older devices earlier
            UserSession s = UserSession.builder()
                    .id(sid)
                    .user(user)
                    .refreshTokenHash(jwtUtil.hashToken("token-" + i))
                    .deviceKey(jwtUtil.hashDeviceKey(userId, "Device-" + i))
                    .deviceName("Device " + i)
                    .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                    .ipAddress("127.0.0.1")
                    .userAgent("Device-" + i)
                    .active(true)
                    .createdAt(now)
                    .lastUsedAt(now)
                    .inactiveExpireAt(now.plusDays(30))
                    .absoluteExpireAt(now.plusDays(90))
                    .rotationCount(0)
                    .build();
            sessionRepository.save(s);
            if (i == 1) oldestSessionId = sid;
        }

        long countBefore = sessionRepository.countByUser_IdAndActiveTrue(userId);
        assertThat(countBefore).isEqualTo(5);

        // Login from 6th device
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD))
                        .header("User-Agent", "Device-6-New"))
                .andExpect(status().isOk())
                .andReturn();

        long countAfter = sessionRepository.countByUser_IdAndActiveTrue(userId);
        assertThat(countAfter).isEqualTo(5); // still 5: oldest evicted, new added

        // Oldest session should be gone (hard deleted)
        assertThat(sessionRepository.findById(oldestSessionId)).isEmpty();
    }

    // ───────────────────────────────────────────────────────────────────────
    // ROTATION COUNT (via authService directly)
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh token rotation increments rotationCount")
    void refreshToken_shouldIncrementRotationCount() throws Exception {
        User user = seedOwner(SEED_PHONE, SEED_PASSWORD);

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(SEED_PHONE, sessionId);
        LocalDateTime now = LocalDateTime.now();

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceKey(jwtUtil.hashDeviceKey(user.getId(), "rotation-test"))
                .deviceName("Test")
                .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                .ipAddress("127.0.0.1")
                .userAgent("rotation-test")
                .active(true)
                .createdAt(now)
                .lastUsedAt(now)
                .inactiveExpireAt(now.plusDays(30))
                .absoluteExpireAt(now.plusDays(90))
                .rotationCount(0)
                .build();
        sessionRepository.save(session);

        // Refresh 3 times
        String currentToken = refreshToken;
        for (int i = 1; i <= 3; i++) {
            AuthService.RefreshResult result = authService.refreshToken(currentToken);
            assertThat(result.isNewRefreshTokenGenerated()).isTrue();
            currentToken = result.getRefreshToken();
        }

        UserSession reloaded = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(reloaded.getRotationCount()).isEqualTo(3);
    }

    // ───────────────────────────────────────────────────────────────────────
    // REVOKED REASON TRACKING
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Logout sets revokedReason to USER_LOGOUT and revokedAt is set")
    void logout_shouldSetRevokedReason() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("sessionId").asText();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Logout
        mockMvc.perform(post(LOGOUT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        UserSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.isActive()).isFalse();
        assertThat(session.getRevokedReason()).isEqualTo("USER_LOGOUT");
        assertThat(session.getRevokedAt()).isNotNull();
    }

    // ───────────────────────────────────────────────────────────────────────
    // ABSOLUTE EXPIRATION
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session past absoluteExpireAt is rejected on refresh")
    void refreshToken_whenAbsoluteExpired_shouldReject() {
        User user = seedOwner(SEED_PHONE, SEED_PASSWORD);

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(SEED_PHONE, sessionId);
        LocalDateTime past = LocalDateTime.now().minusDays(1);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceKey(jwtUtil.hashDeviceKey(user.getId(), "abs-expire-test"))
                .deviceName("Test")
                .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                .ipAddress("127.0.0.1")
                .userAgent("abs-expire-test")
                .active(true)
                .createdAt(past.minusDays(90))
                .lastUsedAt(past)
                .inactiveExpireAt(LocalDateTime.now().plusDays(30))
                .absoluteExpireAt(past)
                .rotationCount(0)
                .build();
        sessionRepository.save(session);

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .hasMessageContaining("Session has reached its maximum lifetime");

        UserSession reloaded = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getRevokedReason()).isEqualTo("ABSOLUTE_EXPIRED");
    }

    // ───────────────────────────────────────────────────────────────────────
    // SLIDING EXPIRATION
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Session past inactiveExpireAt is rejected on refresh")
    void refreshToken_whenInactiveExpired_shouldReject() {
        User user = seedOwner(SEED_PHONE, SEED_PASSWORD);

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(SEED_PHONE, sessionId);
        LocalDateTime past = LocalDateTime.now().minusDays(31);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(jwtUtil.hashToken(refreshToken))
                .deviceKey(jwtUtil.hashDeviceKey(user.getId(), "inact-expire-test"))
                .deviceName("Test")
                .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                .ipAddress("127.0.0.1")
                .userAgent("inact-expire-test")
                .active(true)
                .createdAt(past)
                .lastUsedAt(past)
                .inactiveExpireAt(past)
                .absoluteExpireAt(LocalDateTime.now().plusDays(90))
                .rotationCount(0)
                .build();
        sessionRepository.save(session);

        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .hasMessageContaining("Session has expired due to inactivity");

        UserSession reloaded = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getRevokedReason()).isEqualTo("INACTIVE_EXPIRED");
    }

    // ───────────────────────────────────────────────────────────────────────
    // REUSE DETECTION with revoked reason
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token reuse detection revokes all sessions with REUSE_DETECTED reason")
    void refreshToken_whenReuseDetected_shouldRevokeAllWithReason() {
        User user = seedOwner(SEED_PHONE, SEED_PASSWORD);

        // Create 2 active sessions
        for (int i = 1; i <= 2; i++) {
            String sid = UUID.randomUUID().toString();
            String rt = jwtUtil.generateRefreshToken(SEED_PHONE, sid);
            LocalDateTime now = LocalDateTime.now();
            UserSession s = UserSession.builder()
                    .id(sid)
                    .user(user)
                    .refreshTokenHash(jwtUtil.hashToken(rt))
                    .deviceKey(jwtUtil.hashDeviceKey(user.getId(), "reuse-" + i))
                    .deviceName("Device " + i)
                    .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                    .ipAddress("127.0.0.1")
                    .userAgent("reuse-" + i)
                    .active(true)
                    .createdAt(now)
                    .lastUsedAt(now)
                    .inactiveExpireAt(now.plusDays(30))
                    .absoluteExpireAt(now.plusDays(90))
                    .rotationCount(0)
                    .build();
            sessionRepository.save(s);
        }

        // Create a session whose hash won't match the incoming token
        String sid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        UserSession s = UserSession.builder()
                .id(sid)
                .user(user)
                .refreshTokenHash("different-hash-value")
                .deviceKey(jwtUtil.hashDeviceKey(user.getId(), "reuse-target"))
                .deviceName("Target")
                .deviceType(com.owlexa.owlexabackend.modules.user.entity.DeviceType.DESKTOP)
                .ipAddress("127.0.0.1")
                .userAgent("reuse-target")
                .active(true)
                .createdAt(now)
                .lastUsedAt(now)
                .inactiveExpireAt(now.plusDays(30))
                .absoluteExpireAt(now.plusDays(90))
                .rotationCount(0)
                .build();
        sessionRepository.save(s);

        // Token points to this session but hash won't match
        String mismatchToken = jwtUtil.generateRefreshToken(SEED_PHONE, sid);

        assertThatThrownBy(() -> authService.refreshToken(mismatchToken))
                .hasMessageContaining("Security alert: token reuse detected");

        // All sessions should be deactivated
        long activeCount = sessionRepository.countByUser_IdAndActiveTrue(user.getId());
        assertThat(activeCount).isEqualTo(0);
    }

    // ───────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────

    private User seedOwner(String phone, String password) {
        User u = new User();
        u.setPhoneNumber(phone);
        u.setEmail(phone + "@owlexa.vn");
        u.setFullName("Test Owner");
        u.setPassword(passwordEncoder.encode(password));
        u.setRole(Role.OWNER);
        return userRepository.save(u);
    }

    private Long getUserId(String phone) {
        return userRepository.findByPhoneNumber(phone).orElseThrow().getId();
    }

    private String loginPayload(String phone, String password) {
        return String.format("{\"phoneNumber\":\"%s\",\"password\":\"%s\"}", phone, password);
    }
}
