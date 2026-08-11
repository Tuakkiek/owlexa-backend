package com.owlexa.owlexabackend.common.filter;

import com.owlexa.owlexabackend.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class TenantHibernateFilter extends OncePerRequestFilter {

    public static final String FILTER_NAME = "tenantFilter";
    public static final String PARAM_NAME = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain
    ) throws jakarta.servlet.ServletException, java.io.IOException {

        Long tenantId = TenantContext.getCurrentTenantId();

        if (tenantId != null) {
            entityManager
                    .unwrap(org.hibernate.Session.class)
                    .enableFilter(FILTER_NAME)
                    .setParameter(PARAM_NAME, tenantId);

            log.debug("Enabled tenantFilter for tenantId={}", tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}