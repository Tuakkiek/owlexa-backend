package com.owlexa.owlexabackend.modules.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPermissionOverrideRequest {
    private List<PermissionOverrideItem> overrides;
}
