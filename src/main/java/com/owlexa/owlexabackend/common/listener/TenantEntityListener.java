package com.owlexa.owlexabackend.common.listener;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.context.TenantAware;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantEntityListener {

    private static CenterRepository centerRepository;

    @Autowired
    public void setCenterRepository(CenterRepository centerRepository) {
        TenantEntityListener.centerRepository = centerRepository;
    }

    @PrePersist
    public void prePersist(Object entity) {
        if (!(entity instanceof TenantAware tenantAware)) {
            return;
        }

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            log.warn("No tenant context when persisting {}", entity.getClass().getSimpleName());
            return;
        }

        if (tenantAware.getCenterId() == null) {
            Center center = centerRepository.findById(tenantId).orElse(null);
            if (center != null) {
                setCenterToEntity(entity, center);
                log.debug("Auto-set center_id={} for {}", tenantId, entity.getClass().getSimpleName());
            }
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (!(entity instanceof TenantAware tenantAware)) {
            return;
        }

        Long currentTenantId = TenantContext.getCurrentTenantId();
        Long entityTenantId = tenantAware.getCenterId();

        if (currentTenantId != null && entityTenantId != null && !currentTenantId.equals(entityTenantId)) {
            throw new SecurityException(
                "Attempt to modify entity of another tenant! Entity tenant: " + entityTenantId + ", Current tenant: " + currentTenantId
            );
        }
    }

    private void setCenterToEntity(Object entity, Center center) {
        try {
            var setterMethod = entity.getClass().getMethod("setCenter", Center.class);
            setterMethod.invoke(entity, center);
        } catch (Exception e) {
            log.debug("Could not set center via setCenter method, trying direct field access");
            try {
                var field = entity.getClass().getDeclaredField("center");
                field.setAccessible(true);
                field.set(entity, center);
            } catch (Exception ex) {
                log.warn("Could not set center for {}", entity.getClass().getSimpleName());
            }
        }
    }
}
