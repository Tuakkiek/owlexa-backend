package com.owlexa.owlexabackend.modules.assignment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentItemOptionResponse {

    private Long id;
    private String content;
    private Boolean isCorrect;
    private Integer displayOrder;
}
