package com.owlexa.owlexabackend.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionsResponse {
    private Long userId;
    private String roleName;
    private List<EffectivePermission> permissions;
}
