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
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantHibernateFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @PersistenceContext
    private EntityManager entityManager;

    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Long tenantId = resolveTenantId(request);
            TenantContext.setCurrentTenantId(tenantId);

            if (tenantId != null) {
                enableTenantFilter(tenantId);
                log.debug("Enabled tenant filter for tenantId={}", tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        if (isAdminBypass()) {
            return null;
        }

        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            return resolveFromHeader(tenantHeader);
        }

        return resolveFromAuthenticatedUser();
    }

    private Long resolveFromHeader(String tenantHeader) {
        try {
            return Long.parseLong(tenantHeader.trim());
        } catch (NumberFormatException e) {
            Optional<Center> centerOpt = centerRepository.findBySubdomain(tenantHeader.trim().toLowerCase());
            if (centerOpt.isPresent()) {
                return centerOpt.get().getId();
            }
            return null;
        }
    }

    private Long resolveFromAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank() || "anonymousUser".equals(principalName)) {
            return null;
        }

        User user = userRepository.findByPhoneNumber(principalName).orElse(null);
        if (user != null && user.getRole() == Role.ADMIN) {
            return null;
        }

        if (user != null) {
            List<Membership> memberships = membershipRepository.findAllByUser_Id(user.getId());
            if (memberships.size() == 1) {
                return memberships.get(0).getCenter().getId();
            }
        }

        return null;
    }

    private boolean isAdminBypass() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String principalName = authentication.getName();
        if ("anonymousUser".equals(principalName)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByPhoneNumber(principalName);
        return userOpt.isPresent() && userOpt.get().getRole() == Role.ADMIN;
    }

    private void enableTenantFilter(Long tenantId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
            || path.startsWith("/actuator/")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }
}
