package com.owlexa.owlexabackend.modules.assessment_builder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentBlockRequest {
    private Long id;
    private Integer position;
    private String title;
    private JsonNode content;
}
