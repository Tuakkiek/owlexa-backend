package com.owlexa.owlexabackend.modules.assignment.dto.response;

import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentTargetResponse {

    private Long id;
    private AssignmentTargetType targetType;
    private Long classId;
    private String className;
    private Long studentUserId;
    private String studentFullName;
}
