package com.owlexa.owlexabackend.common.filter;

import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
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

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainResolverFilterTest {

    @Mock private CenterRepository centerRepository;
    @Mock private FilterChain filterChain;

    private DomainResolverFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DomainResolverFilter(centerRepository);
    }

    @AfterEach
    void tearDown() {
        // không có ThreadLocal, không cần clear
    }

    private Center buildCenter(Long id, String subdomain) {
        Center center = new Center();
        center.setId(id);
        center.setSubdomain(subdomain);
        return center;
    }

    @Test
    @DisplayName("Subdomain hợp lệ → set request attribute và pass chain")
    void doFilterInternal_whenValidSubdomain_shouldSetAttributeAndContinue() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "hanoi.owlexa.vn");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Center center = buildCenter(77L, "hanoi");
        when(centerRepository.findBySubdomain("hanoi")).thenReturn(Optional.of(center));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(DomainResolverFilter.ATTR_RESOLVED_CENTER_ID)).isEqualTo(77L);
        assertThat(request.getAttribute(DomainResolverFilter.ATTR_RESOLVED_CENTER)).isEqualTo(center);
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Subdomain không tồn tại → trả 404 JSON, KHÔNG gọi chain")
    void doFilterInternal_whenSubdomainNotFound_shouldReturn404() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "unknown.owlexa.vn");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(centerRepository.findBySubdomain("unknown")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("unknown");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Host header thiếu → pass chain, KHÔNG query DB")
    void doFilterInternal_whenHostHeaderMissing_shouldPassChainWithoutQuerying() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Localhost → pass chain, KHÔNG query DB")
    void doFilterInternal_whenHostIsLocalhost_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "localhost:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Subdomain dev → pass chain (không phải tenant)")
    void doFilterInternal_whenSubdomainIsDev_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "dev.owlexa.vn");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Root domain (owlexa.vn) → pass chain, KHÔNG query DB")
    void doFilterInternal_whenHostIsRootDomain_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "owlexa.vn");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Host có port → vẫn extract subdomain đúng")
    void doFilterInternal_whenHostHasPort_shouldStillExtractSubdomain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "hcmc.owlexa.vn:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Center center = buildCenter(88L, "hcmc");
        when(centerRepository.findBySubdomain("hcmc")).thenReturn(Optional.of(center));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(DomainResolverFilter.ATTR_RESOLVED_CENTER_ID)).isEqualTo(88L);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Subdomain lồng nhau (vd api.hanoi.owlexa.vn) → KHÔNG query DB")
    void doFilterInternal_whenSubdomainIsMultiLevel_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "api.hanoi.owlexa.vn");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Domain khác owlexa.vn → pass chain")
    void doFilterInternal_whenHostIsForeignDomain_shouldPassChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(centerRepository, never()).findBySubdomain(org.mockito.ArgumentMatchers.anyString());
        verify(filterChain).doFilter(request, response);
    }
}