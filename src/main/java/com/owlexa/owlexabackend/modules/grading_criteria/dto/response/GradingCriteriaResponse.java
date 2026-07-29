package com.owlexa.owlexabackend.modules.grading_criteria.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingCriteriaResponse {

    private Long id;
    private String name;
    private JsonNode content;
    private Instant createdAt;
    private Instant updatedAt;
}
