package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDetailResponse {

    private Long id;
    private AssessmentType type;
    private AssessmentStatus status;
    private String title;
    private String description;
    private List<AssessmentItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}
