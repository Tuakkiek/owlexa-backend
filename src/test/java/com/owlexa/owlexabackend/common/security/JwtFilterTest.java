package com.owlexa.owlexabackend.common.security;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private FilterChain filterChain;

    private JwtFilter filter;

    private static final String ACCESS_TOKEN = "valid.access.token";
    private static final String REFRESH_TOKEN = "valid.refresh.token";
    private static final String PHONE = "0901234567";
    private static final String SESSION_ID = "sess-123";

    @BeforeEach
    void setUp() {
        filter = new JwtFilter(jwtUtil, userDetailsService, sessionRepository, userRepository);
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (token != null) {
            req.addHeader("Authorization", "Bearer " + token);
        }
        return req;
    }

    private UserDetails mockUserDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername(PHONE)
                .password("")
                .authorities(java.util.List.of())
                .build();
    }

    @Test
    @DisplayName("Không có Authorization header → pass chain, không set SecurityContext")
    void doFilterInternal_whenNoAuthHeader_shouldPassChainWithoutSettingContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization header không bắt đầu bằng 'Bearer ' → pass chain")
    void doFilterInternal_whenAuthHeaderIsNotBearer_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abcdef");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Refresh token → pass chain KHÔNG set SecurityContext (xử lý ở controller refresh)")
    void doFilterInternal_whenRefreshToken_shouldPassChainWithoutAuth() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(REFRESH_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessionRepository, never()).existsByIdAndActiveTrue(SESSION_ID);
    }

    @Test
    @DisplayName("Access token hợp lệ + session active → set SecurityContext")
    void doFilterInternal_whenValidAccessTokenAndActiveSession_shouldSetSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(sessionRepository.existsByIdAndActiveTrue(SESSION_ID)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(PHONE)).thenReturn(mockUserDetails());

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(PHONE);
        assertThat(request.getAttribute("currentSessionId")).isEqualTo(SESSION_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Access token hợp lệ nhưng session inactive → pass chain KHÔNG set context")
    void doFilterInternal_whenSessionInactive_shouldPassChainWithoutAuth() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(sessionRepository.existsByIdAndActiveTrue(SESSION_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(PHONE);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token thiếu subject → pass chain, không set context")
    void doFilterInternal_whenTokenMissingSubject_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(null);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(sessionRepository, never()).existsByIdAndActiveTrue(SESSION_ID);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token thiếu sessionId → pass chain, không set context")
    void doFilterInternal_whenTokenMissingSessionId_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(sessionRepository, never()).existsByIdAndActiveTrue(SESSION_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JwtUtil throw exception (token invalid) → catch, pass chain, không crash")
    void doFilterInternal_whenJwtUtilThrows_shouldCatchAndPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer("malformed.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken("malformed.token")).thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("UserDetailsService throw UsernameNotFoundException → catch, pass chain")
    void doFilterInternal_whenUserNotFound_shouldCatchAndPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(sessionRepository.existsByIdAndActiveTrue(SESSION_ID)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(PHONE))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Đã có authentication trong SecurityContext → KHÔNG ghi đè (giữ context cũ)")
    void doFilterInternal_whenAuthAlreadyInContext_shouldNotOverwrite() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "different-user", null, java.util.List.of()
                );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("different-user");
        verify(userDetailsService, never()).loadUserByUsername(PHONE);
    }

    @Test
    @DisplayName("User có centerId → set TenantContext (kiểm tra cơ chế set tenant từ user)")
    void doFilterInternal_whenUserHasCenterId_shouldSetTenantContext() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(sessionRepository.existsByIdAndActiveTrue(SESSION_ID)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(PHONE)).thenReturn(mockUserDetails());

        User userWithCenter = new User() {
            @Override
            public Long getCenterId() {
                return 42L;
            }
        };
        userWithCenter.setId(1L);
        userWithCenter.setPhoneNumber(PHONE);
        lenient().when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(userWithCenter));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("User KHÔNG có centerId (getCenterId trả null) → TenantContext KHÔNG được set")
    void doFilterInternal_whenUserHasNoCenterId_shouldNotSetTenantContext() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearer(ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);
        when(jwtUtil.extractSubject(ACCESS_TOKEN)).thenReturn(PHONE);
        when(jwtUtil.extractSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(sessionRepository.existsByIdAndActiveTrue(SESSION_ID)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(PHONE)).thenReturn(mockUserDetails());

        User userNoCenter = new User() {
            @Override
            public Long getCenterId() {
                return null;
            }
        };
        userNoCenter.setId(1L);
        userNoCenter.setPhoneNumber(PHONE);
        lenient().when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(userNoCenter));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}