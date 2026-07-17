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
     *   <li>{@code ALLOW} — explicitly grant this permission</li>
     *   <li>{@code DENY} — explicitly revoke this permission</li>
     *   <li>{@code INHERIT} — remove any override; fall back to role default</li>
     * </ul>
     */
    private String type;
}
