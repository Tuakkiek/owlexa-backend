package com.owlexa.owlexabackend.modules.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinglePermissionOverrideRequest {

    /**
     * Override type: {@code DISABLED} to revoke, or {@code INHERIT} to restore.
     * ALLOW and DENY are accepted for backward compatibility.
     */
    private String type;
}
