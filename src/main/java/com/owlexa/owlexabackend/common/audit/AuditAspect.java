package com.owlexa.owlexabackend.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// FIXME: Requires @AuditLog annotation to be created. Disabled until annotation is defined.
// @Aspect
// @Component
@Slf4j
public class AuditAspect {

    @AfterReturning("@annotation(auditLog)")
    public void logAuditActivity(JoinPoint joinPoint, AuditLog auditLog) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "system";
        String action = auditLog.action();
        String methodName = joinPoint.getSignature().getName();

        log.info("AUDIT: User '{}' successfully performed action '{}' via method '{}'.", username, action, methodName);
    }
}
