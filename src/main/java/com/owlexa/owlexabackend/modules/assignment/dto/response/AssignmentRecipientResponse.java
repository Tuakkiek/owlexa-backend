package com.owlexa.owlexabackend.modules.assignment.dto.response;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRecipientResponse {

    private Long id;
    private Long studentUserId;
    private String studentFullName;
    private Long classId;
    private String className;
    private AssignmentTargetType sourceType;
    private AssignmentRecipientStatus status;
    private Instant assignedAt;
}
