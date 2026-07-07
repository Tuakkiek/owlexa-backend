package com.owlexa.owlexabackend.common.filter;

import com.owlexa.owlexabackend.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cho TenantHibernateFilter theo kiến trúc mới (sau refactor):
 *
 *   JWT request flow:
 *   DomainResolverFilter (resolve subdomain → resolvedCenterId)
 *       → JwtFilter (parse JWT → set SecurityContext → set TenantContext từ user.centerId)
 *           → TenantHibernateFilter (enable Hibernate Filter nếu TenantContext có value)
 *               → Controller
 *
 *   TenantHibernateFilter KHÔNG tự resolve center.
 *   Nó chỉ đọc TenantContext (do JwtFilter set) và enable filter Hibernate tương ứng.
 *   Việc clear ThreadLocal ở finally là bắt buộc để chống leak khi Tomcat dùng thread pool.
 */
@ExtendWith(MockitoExtension.class)
class TenantHibernateFilterTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter hibernateFilter;

    private TenantHibernateFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new TenantHibernateFilter();

        // Inject mock EntityManager vào field private (vì @PersistenceContext không hoạt động trong unit test)
        var field = TenantHibernateFilter.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(filter, entityManager);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantContext có value → bật Hibernate Filter với tenantId tương ứng")
    void doFilterInternal_whenTenantContextHasValue_shouldEnableTenantFilter() throws ServletException, IOException {
        TenantContext.setCurrentTenantId(77L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantHibernateFilter.FILTER_NAME)).thenReturn(hibernateFilter);

        FilterChain chain = (req, res) -> {
            // Trong lúc controller chạy, filter đã được enable
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(77L);
        };

        filter.doFilterInternal(mockRequest(), mockResponse(), chain);

        verify(session).enableFilter(TenantHibernateFilter.FILTER_NAME);
        verify(hibernateFilter).setParameter(TenantHibernateFilter.PARAM_NAME, 77L);
    }

    @Test
    @DisplayName("TenantContext null (public endpoint) → KHÔNG bật Hibernate Filter")
    void doFilterInternal_whenTenantContextIsNull_shouldNotEnableFilter() throws ServletException, IOException {
        FilterChain chain = (req, res) -> {
            // Đảm bảo controller vẫn chạy bình thường, không bị filter áp đặt
            assertThat(TenantContext.getCurrentTenantId()).isNull();
        };

        filter.doFilterInternal(mockRequest(), mockResponse(), chain);

        verify(entityManager, never()).unwrap(Session.class);
        verify(session, never()).enableFilter(TenantHibernateFilter.FILTER_NAME);
    }

    @Test
    @DisplayName("Sau request, TenantContext PHẢI được clear (chống leak thread pool)")
    void doFilterInternal_shouldClearTenantContextAfterRequest() throws ServletException, IOException {
        TenantContext.setCurrentTenantId(99L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantHibernateFilter.FILTER_NAME)).thenReturn(hibernateFilter);

        FilterChain chain = (req, res) -> { /* no-op */ };

        filter.doFilterInternal(mockRequest(), mockResponse(), chain);

        assertThat(TenantContext.getCurrentTenantId())
                .as("TenantContext phải null sau request, nếu không request kế tiếp dùng lại thread sẽ đọc nhầm tenant")
                .isNull();
    }

    @Test
    @DisplayName("Ngay cả khi controller throw exception, TenantContext vẫn phải được clear trong finally")
    void doFilterInternal_shouldClearTenantContextEvenWhenFilterChainThrows() {
        TenantContext.setCurrentTenantId(123L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantHibernateFilter.FILTER_NAME)).thenReturn(hibernateFilter);

        FilterChain throwingChain = (req, res) -> {
            throw new RuntimeException("Controller failure");
        };

        try {
            filter.doFilterInternal(mockRequest(), mockResponse(), throwingChain);
        } catch (Exception ignored) {
            // Mong đợi exception được propagate lên
        }

        assertThat(TenantContext.getCurrentTenantId())
                .as("Nếu finally không chạy, bug nghiêm trọng: thread pool leak tenantId")
                .isNull();
    }

    private HttpServletRequest mockRequest() {
        return new org.springframework.mock.web.MockHttpServletRequest();
    }

    private HttpServletResponse mockResponse() {
        return new org.springframework.mock.web.MockHttpServletResponse();
    }
}