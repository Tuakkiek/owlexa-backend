package com.owlexa.owlexabackend.modules.student_submission.dto.response;

import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentBlockResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
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
public class TeacherAttemptDetailResponse {

    private Long id;
    private Long assignmentId;
    private Long assignmentRecipientId;
    private Long studentUserId;
    private String studentFullName;
    private Long classId;
    private String className;
    private AssignmentTargetType sourceType;
    private AssignmentRecipientStatus recipientStatus;
    private String assignmentTitleSnapshot;
    private JsonNode assignmentContent;
    private SubmissionAttemptStatus status;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant lastSavedAt;
    private Instant submittedAt;
    private BigDecimal autoScore;
    private BigDecimal maxScore;
    private List<SubmissionAttemptItemResponse> items;
    private List<SubmissionAnswerResponse> answers;
    private List<AssignmentBlockResponse> blocks;
}
