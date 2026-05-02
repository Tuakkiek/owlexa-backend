package com.owlexa.owlexabackend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

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
                    // Header sai format → trả lỗi 400
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid X-Tenant-ID header");
                    return;
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
}
