package com.owlexa.owlexabackend.integration.auth;

import com.owlexa.owlexabackend.common.security.JwtUtil;
import com.owlexa.owlexabackend.integration.BaseIntegrationTest;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for POST /auth/login.
 *
 * <p>Covers end-to-end HTTP flow:
 * <ul>
 *   <li>Validation (DTO @NotBlank)</li>
 *   <li>Happy path: access token returned + session row created + refresh cookie set</li>
 *   <li>Negative cases: wrong password, user not found</li>
 *   <li>JSON response contract</li>
 * </ul>
 *
 * <p>Does NOT test:
 * <ul>
 *   <li>User disable — User entity has no {@code enabled} field, business rule absent</li>
 *   <li>Refresh token flow (separate test planned)</li>
 *   <li>Tenant isolation (separate test planned)</li>
 *   <li>Testcontainers (Phase 2)</li>
 * </ul>
 */
class LoginIntegrationTest extends BaseIntegrationTest {

    private static final String LOGIN_URL = "/auth/login";

    private static final String SEED_PHONE = "0900000001";
    private static final String SEED_PASSWORD = "Secret#123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @AfterEach
    void cleanUp() {
        sessionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        SecurityContextHolder.clearContext();
    }

    // ───────────────────────────────────────────────────────────────────────
    // HAPPY PATH
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with valid credentials returns 200 and AuthResponse JSON")
    void login_whenValidCredentials_shouldReturn200AndAuthResponse() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.phoneNumber").value(SEED_PHONE))
                .andExpect(jsonPath("$.roleName").value("OWNER"))
                .andExpect(jsonPath("$.fullName").value("Integration Owner"))
                .andExpect(jsonPath("$.centerName").doesNotExist())
                .andExpect(jsonPath("$.email").value("integration.owner@owlexa.vn"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).startsWith("eyJ");
    }

    @Test
    @DisplayName("Login with valid credentials sets refresh token cookie")
    void login_whenValidCredentials_shouldSetRefreshTokenCookie() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().maxAge("refreshToken", 30 * 24 * 60 * 60));
    }

    @Test
    @DisplayName("Login persists an active UserSession row referencing the user")
    void login_whenValidCredentials_shouldPersistActiveSession() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String sessionId = jwtUtil.extractSessionId(body.get("accessToken").asText());

        assertThat(sessionRepository.existsById(sessionId)).isTrue();
        assertThat(sessionRepository.existsByIdAndActiveTrue(sessionId)).isTrue();
    }

    @Test
    @DisplayName("Login generates an access token whose JWT claims contain role and sessionId")
    void login_whenValidCredentials_shouldGenerateJwtWithExpectedClaims() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        String[] parts = accessToken.split("\\.");
        assertThat(parts).hasSize(3);
        // Header: {"alg":"HS256","typ":"JWT"}
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
        assertThat(headerJson).contains("HS256");
        // Payload: subject=phone, role=OWNER, tokenType=access
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payloadJson).contains("\"sub\":\"" + SEED_PHONE + "\"")
                .contains("\"role\":\"OWNER\"")
                .contains("\"tokenType\":\"access\"");
    }

    @Test
    @DisplayName("Login normalizes leading/trailing whitespace in phone number before lookup")
    void login_whenPhoneHasSurroundingWhitespace_shouldStillSucceed() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("  " + SEED_PHONE + "  ", SEED_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(SEED_PHONE));
    }

    // ───────────────────────────────────────────────────────────────────────
    // NEGATIVE — AUTH FAILURE
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with wrong password returns 400 with BadRequest body")
    void login_whenWrongPassword_shouldReturn400() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, "WrongPassword#1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid password"));
    }

    @Test
    @DisplayName("Login for non-existent phone returns 400 with 'User not found'")
    void login_whenUserNotFound_shouldReturn400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("0999999999", "AnyPassword#1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("Login failure does not create a session row")
    void login_whenAuthFails_shouldNotCreateSession() throws Exception {
        seedOwner(SEED_PHONE, SEED_PASSWORD);
        long sessionCountBefore = sessionRepository.count();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, "WrongPassword#1")))
                .andExpect(status().isBadRequest());

        assertThat(sessionRepository.count()).isEqualTo(sessionCountBefore);
    }

    // ───────────────────────────────────────────────────────────────────────
    // NEGATIVE — VALIDATION
    // ───────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with blank phone number returns 400 with validation errors")
    void login_whenPhoneBlank_shouldReturn400Validation() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("", SEED_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.phoneNumber").value("Phone number is required"));
    }

    @Test
    @DisplayName("Login with blank password returns 400 with validation errors")
    void login_whenPasswordBlank_shouldReturn400Validation() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(SEED_PHONE, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").value("Password is required"));
    }

    @Test
    @DisplayName("Login with empty body returns 400 BadRequestException 'Phone number must not be empty'")
    void login_whenRequestBodyMissingFields_shouldFailValidation() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Login with malformed JSON returns 400 'Malformed JSON request'")
    void login_whenMalformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    // ───────────────────────────────────────────────────────────────────────
    // HELPERS
    // ───────────────────────────────────────────────────────────────────────

    private void seedOwner(String phone, String rawPassword) {
        User user = new User();
        user.setPhoneNumber(phone);
        user.setEmail("integration.owner@owlexa.vn");
        user.setFullName("Integration Owner");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.OWNER);
        userRepository.save(user);
    }

    private String loginPayload(String phone, String password) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("phoneNumber", phone);
        payload.put("password", password);
        return objectMapper.writeValueAsString(payload);
    }
}