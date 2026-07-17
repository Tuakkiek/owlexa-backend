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
     * Override type: {@code ALLOW}, {@code DENY}, or {@code INHERIT}.
     */
    private String type;
}
