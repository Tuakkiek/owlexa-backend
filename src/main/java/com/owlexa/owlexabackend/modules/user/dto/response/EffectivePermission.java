package com.owlexa.owlexabackend.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EffectivePermission {

    private String code;
    private String description;

    /**
     * Permission state:
     * <ul>
     *   <li>{@code ENABLED} — permission is active (inherited from role, not disabled)</li>
     *   <li>{@code DISABLED} — permission has been revoked by the Owner</li>
     * </ul>
     */
    private String source;
}
