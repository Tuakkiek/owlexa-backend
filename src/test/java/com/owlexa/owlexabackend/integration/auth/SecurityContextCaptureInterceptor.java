package com.owlexa.owlexabackend.integration.auth;

import com.owlexa.owlexabackend.common.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Test-only interceptor that snapshots SecurityContextHolder and TenantContext
 * at the moment a controller is invoked. Lets integration tests assert
 * what the JwtFilter set, even though the SecurityContextHolderFilter
 * clears the context after the request returns.
 *
 * <p>Registered as a Spring bean via {@code @TestConfiguration} on the
 * AccessTokenIntegrationTest class. Spring Boot's MVC auto-config picks
 * all {@code HandlerInterceptor} beans and adds them to the interceptor registry.
 *
 * <p>This is the cleanest non-bypass mechanism to observe the post-filter
 * state of SecurityContextHolder and TenantContext inside an integration test.
 */
public class SecurityContextCaptureInterceptor implements HandlerInterceptor {

    /** Volatile because the test thread and the request thread are the same MockMvc thread,
     *  but captured as a static field to avoid leaking instances. */
    public static volatile Authentication lastAuthentication;
    public static volatile Long lastTenantId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        lastAuthentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        lastTenantId = TenantContext.getCurrentTenantId();
        return true;
    }
}