package com.owlexa.owlexabackend.modules.assignment.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentListResponse {

    private Long id;
    private Long assessmentId;
    private AssessmentType type;
    private AssignmentStatus status;
    private String title;
    private String description;
    private JsonNode content;
    private Instant openAt;
    private Instant dueAt;
    private Integer attemptLimit;
    private Instant assessmentSnapshotAt;
    private Instant createdAt;
    private Instant updatedAt;
}
