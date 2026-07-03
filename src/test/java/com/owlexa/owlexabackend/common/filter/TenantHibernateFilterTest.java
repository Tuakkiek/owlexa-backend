package com.owlexa.owlexabackend.common.filter;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantHibernateFilterTest {

    @Mock
    private CenterRepository centerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Session session;

    private TenantHibernateFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantHibernateFilter(centerRepository, userRepository, membershipRepository);
        setEntityManager(field -> {
            if (field.getName().equals("entityManager")) {
                try {
                    field.setAccessible(true);
                    field.set(filter, entityManager);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void setEntityManager(java.util.function.Consumer<java.lang.reflect.Field> setter) {
        try {
            var field = TenantHibernateFilter.class.getDeclaredField("entityManager");
            field.setAccessible(true);
            field.set(filter, entityManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void doFilterInternal_whenHeaderPresent_shouldEnableTenantFilter() throws Exception {
        when(entityManager.unwrap(Session.class)).thenReturn(session);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "77");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(77L);
        };

        filter.doFilterInternal(request, response, chain);

        verify(session).enableFilter("tenantFilter");
        verify(session.enableFilter("tenantFilter")).setParameter("tenantId", 77L);
    }

    @Test
    void doFilterInternal_whenUserHasSingleMembership_shouldUseMembershipCenter() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setPhoneNumber("0901234567");
        user.setRole(Role.OWNER);

        Center center = new Center();
        center.setId(77L);

        Membership membership = new Membership();
        membership.setCenter(center);
        membership.setUser(user);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(membershipRepository.findAllByUser_Id(10L)).thenReturn(List.of(membership));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("0901234567", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(77L);
        };

        filter.doFilterInternal(request, response, chain);
    }

    @Test
    void doFilterInternal_whenAdminUser_shouldBypassFilter() throws Exception {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setPhoneNumber("admin");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByPhoneNumber("admin")).thenReturn(Optional.of(adminUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(TenantContext.getCurrentTenantId()).isNull();
        };

        filter.doFilterInternal(request, response, chain);

        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void doFilterInternal_shouldClearContextAfterRequest() throws Exception {
        when(entityManager.unwrap(Session.class)).thenReturn(session);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "99");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(99L);
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}
