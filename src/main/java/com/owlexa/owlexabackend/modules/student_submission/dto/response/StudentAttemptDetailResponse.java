package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentBlockResponse;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAttemptDetailResponse {

    private Long id;
    private Long assignmentId;
    private Long assignmentRecipientId;
    private String assignmentTitleSnapshot;
    private JsonNode assignmentContent;
    private SubmissionAttemptStatus status;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant lastSavedAt;
    private Instant submittedAt;
    private BigDecimal autoScore;
    private BigDecimal maxScore;
    private FileResponse audioFile;
    private PlaybackMode playbackMode;
    private List<StudentAttemptItemResponse> items;
    private List<SubmissionAnswerResponse> answers;
    private List<AssignmentBlockResponse> blocks;
    private Boolean showScore;
    private Boolean allowReview;
    private Boolean hasPassword;
    private StudentAIGradingResultResponse aiResult;
}
