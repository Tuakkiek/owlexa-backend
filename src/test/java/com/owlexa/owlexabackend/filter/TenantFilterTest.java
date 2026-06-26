package com.owlexa.owlexabackend.filter;

import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    @Test
    void doFilterInternal_whenHeaderMissingAndUserHasSingleMembership_shouldUseMembershipCenter() throws Exception {
        TenantFilter filter = new TenantFilter();
        CenterRepository centerRepository = mock(CenterRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipRepository membershipRepository = mock(MembershipRepository.class);

        ReflectionTestUtils.setField(filter, "centerRepository", centerRepository);
        ReflectionTestUtils.setField(filter, "userRepository", userRepository);
        ReflectionTestUtils.setField(filter, "membershipRepository", membershipRepository);

        User user = new User();
        user.setId(10L);
        user.setPhoneNumber("0901234567");

        Center center = new Center();
        center.setId(77L);

        Membership membership = new Membership();
        membership.setCenter(center);
        membership.setUser(user);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(user));
        when(membershipRepository.findAllByUserId(10L)).thenReturn(List.of(membership));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("0901234567", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicLong capturedCenterId = new AtomicLong(-1L);
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse)
                    throws IOException, ServletException {
                capturedCenterId.set(TenantFilter.getCurrentCenterId() == null ? -1L : TenantFilter.getCurrentCenterId());
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(capturedCenterId.get()).isEqualTo(77L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantFilter.clearCurrentCenterId();
    }
}
