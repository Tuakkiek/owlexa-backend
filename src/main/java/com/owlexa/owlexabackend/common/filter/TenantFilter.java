package com.owlexa.owlexabackend.common.filter;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
public class TenantFilter extends OncePerRequestFilter {

    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public TenantFilter(CenterRepository centerRepository,
                        UserRepository userRepository,
                        MembershipRepository membershipRepository) {
        this.centerRepository = centerRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    private static final ThreadLocal<Long> currentCenterId = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException{

        String tenantId = request.getHeader("X-Tenant-ID");

        try {
            if (tenantId != null && !tenantId.isBlank()) {
                try {
                    currentCenterId.set(Long.parseLong(tenantId));
                } catch (NumberFormatException e) {
                    // Nếu không phải là số, coi như là subdomain và tìm trong DB
                    Optional<Center> centerOpt = centerRepository.findBySubdomain(tenantId.trim().toLowerCase());
                    if (centerOpt.isPresent()) {
                        currentCenterId.set(centerOpt.get().getId());
                    } else if (!"default".equalsIgnoreCase(tenantId.trim())) {
                        // Nếu không phải default và không tìm thấy Center tương ứng
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("Center not found for subdomain: " + tenantId);
                        return;
                    }
                }
            } else {
                resolveTenantFromAuthenticatedUser();
            }
            chain.doFilter(request, response);
        } finally {
            currentCenterId.remove();
        }
    }

    private void resolveTenantFromAuthenticatedUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        String principalName = authentication.getName();
        if (principalName == null || principalName.isBlank() || "anonymousUser".equals(principalName)) {
            return;
        }

        Optional<User> userOpt = userRepository.findByPhoneNumber(principalName);
        if (userOpt.isEmpty()) {
            return;
        }

        List<Membership> memberships = membershipRepository.findAllByUserId(userOpt.get().getId());
        if (memberships.size() == 1) {
            currentCenterId.set(memberships.get(0).getCenter().getId());
        }
    }

    public static Long getCurrentCenterId() {
        return currentCenterId.get();
    }

    public static void clearCurrentCenterId() {
        currentCenterId.remove();
    }

    public static void setCurrentCenterIdForTest(Long centerId) {
        currentCenterId.set(centerId);
    }
}
