package com.owlexa.owlexabackend.modules.assessment_builder.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDetailResponse {

    private Long id;
    private AssessmentStatus status;
    private String title;
    private String description;
    private JsonNode content;
    private Long audioFileId;
    private FileResponse audioFile;
    private PlaybackMode playbackMode;
    private List<AssessmentItemResponse> items;
    private List<AssessmentBlockResponse> blocks;
    private Instant createdAt;
    private Instant updatedAt;
}
