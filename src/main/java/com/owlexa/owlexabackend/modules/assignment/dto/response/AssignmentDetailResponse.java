package com.owlexa.owlexabackend.modules.assignment.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
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
public class AssignmentDetailResponse {

    private Long id;
    private Long assessmentId;
    private AssessmentType type;
    private AssignmentStatus status;
    private String title;
    private String description;
    private Instant openAt;
    private Instant dueAt;
    private Integer attemptLimit;
    private Instant assessmentSnapshotAt;
    private FileResponse audioFile;
    private PlaybackMode playbackMode;
    private List<AssignmentTargetResponse> targets;
    private List<AssignmentRecipientResponse> recipients;
    private List<AssignmentItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}
