package com.owlexa.owlexabackend.modules.assignment.dto.response;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAssignmentListResponse {

    private Long id;
    private Long recipientId;
    private AssignmentStatus status;
    private AssignmentRecipientStatus recipientStatus;
    private String title;
    private String description;
    private Instant openAt;
    private Instant dueAt;
    private Integer attemptLimit;
    private Boolean showScore;
    private Boolean allowReview;
    private Boolean hasPassword;
    private Instant assignedAt;
}
