package com.owlexa.owlexabackend.modules.assignment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentBlockResponse {
    private Long id;
    private Integer position;
    private String title;
    private JsonNode content;
}
