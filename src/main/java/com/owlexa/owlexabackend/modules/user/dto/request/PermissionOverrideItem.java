package com.owlexa.owlexabackend.modules.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionOverrideItem {

    private String permissionCode;

    /**
     * Override type:
     * <ul>
     *   <li>{@code DISABLED} — revoke this permission</li>
     *   <li>{@code INHERIT} — restore this permission (re-enable)</li>
     * </ul>
     * Note: ALLOW and DENY are still accepted for backward compatibility.
     */
    private String type;
}
