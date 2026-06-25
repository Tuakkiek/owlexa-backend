package com.owlexa.owlexabackend.filter;

import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.repository.CenterRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;


@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private CenterRepository centerRepository;

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
            }
            chain.doFilter(request, response);
        } finally {
            currentCenterId.remove();
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
