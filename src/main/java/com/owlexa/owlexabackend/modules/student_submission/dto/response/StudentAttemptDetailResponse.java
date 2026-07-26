package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private AssessmentType assignmentTypeSnapshot;
    private SubmissionAttemptStatus status;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant lastSavedAt;
    private Instant submittedAt;
    private BigDecimal autoScore;
    private BigDecimal maxScore;
    private List<SubmissionAttemptItemResponse> items;
    private List<SubmissionAnswerResponse> answers;
}
