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
     * Where this permission comes from:
     * <ul>
     *   <li>{@code ROLE_DEFAULT} — inherited from the user's role via role_permission</li>
     *   <li>{@code ALLOW} — explicitly granted via user_permission (type = ALLOW)</li>
     *   <li>{@code DENY} — explicitly revoked via user_permission (type = DENY)</li>
     * </ul>
     */
    private String source;
}
