package com.owlexa.owlexabackend.modules.assignment.dto.request;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentTargetRequest {

    @NotNull(message = "Target type is required")
    private AssignmentTargetType targetType;

    private Long classId;

    private Long studentUserId;
}
