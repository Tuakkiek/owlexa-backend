package com.owlexa.owlexabackend.modules.class_management.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterResponse {
    private Long id;
    private String name;
    private String subdomain;
    private Instant createdAt;
}
