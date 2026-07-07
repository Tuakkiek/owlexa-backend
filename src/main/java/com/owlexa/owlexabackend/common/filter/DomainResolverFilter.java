package com.owlexa.owlexabackend.common.filter;

import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter chạy đầu tiên trong chain.
 * Nhiệm vụ:
 *  1. Đọc Host header (vd "hanoi.owlexa.vn").
 *  2. Tách subdomain (vd "hanoi").
 *  3. Tra CenterRepository → tìm Center tương ứng.
 *  4. Set request attribute "resolvedCenterId" để Controller sử dụng.
 *
 * Lưu ý: KHÔNG set TenantContext ở đây.
 * Guest request chưa authenticate, việc enable Hibernate Filter
 * là quyết định của tầng Controller / Service sau khi xác định
 * request có hợp lệ truy cập dữ liệu center hay không.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DomainResolverFilter extends OncePerRequestFilter {

    public static final String ATTR_RESOLVED_CENTER_ID = "resolvedCenterId";
    public static final String ATTR_RESOLVED_CENTER = "resolvedCenter";
    private static final String LOCALHOST = "localhost";
    private static final String OWLEXA_ROOT_DOMAIN = "owlexa.vn";
    private static final String DEV_SUBDOMAIN = "dev";

    private final CenterRepository centerRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String host = request.getHeader("Host");
        if (host == null) {
            chain.doFilter(request, response);
            return;
        }

        String subdomain = extractSubdomain(host);
        if (subdomain == null || subdomain.equals(LOCALHOST) || subdomain.equals(DEV_SUBDOMAIN)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Center> centerOpt = centerRepository.findBySubdomain(subdomain);
        if (centerOpt.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":404,\"message\":\"Center not found for subdomain: " + subdomain + "\"}"
            );
            return;
        }

        Center center = centerOpt.get();
        request.setAttribute(ATTR_RESOLVED_CENTER_ID, center.getId());
        request.setAttribute(ATTR_RESOLVED_CENTER, center);
        log.debug("Resolved subdomain={} -> centerId={}", subdomain, center.getId());

        chain.doFilter(request, response);
    }

    /**
     * Tách subdomain từ Host header.
     * - "hanoi.owlexa.vn"     → "hanoi"
     * - "owlexa.vn"           → null (root domain, không phải tenant)
     * - "localhost"           → null
     * - "hanoi.owlexa.vn:8080" → "hanoi"
     */
    private String extractSubdomain(String host) {
        String cleaned = host.split(":")[0];
        if (!cleaned.endsWith("." + OWLEXA_ROOT_DOMAIN)) {
            return null;
        }
        String prefix = cleaned.substring(0, cleaned.length() - OWLEXA_ROOT_DOMAIN.length() - 1);
        if (prefix.isBlank() || prefix.contains(".")) {
            return null;
        }
        return prefix;
    }
}