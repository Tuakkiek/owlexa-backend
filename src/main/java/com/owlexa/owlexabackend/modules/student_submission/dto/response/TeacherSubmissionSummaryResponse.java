package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubmissionSummaryResponse {

    private Long recipientId;
    private Long studentUserId;
    private String studentFullName;
    private Long classId;
    private String className;
    private AssignmentTargetType sourceType;
    private AssignmentRecipientStatus recipientStatus;
    private Long latestAttemptId;
    private Integer latestAttemptNumber;
    private SubmissionAttemptStatus latestStatus;
    private Instant latestStartedAt;
    private Instant latestSubmittedAt;
    private BigDecimal latestAutoScore;
    private BigDecimal latestFinalScore;
    private Boolean isGraded;
    private BigDecimal maxScore;
    private Long attemptsCount;
}
