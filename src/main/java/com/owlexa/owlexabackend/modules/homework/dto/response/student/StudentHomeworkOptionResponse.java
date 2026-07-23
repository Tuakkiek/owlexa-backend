package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

@Data
public class StudentHomeworkOptionResponse {
    private Long id;
    private String content;
    private Integer sortOrder;
    // NOTE: isCorrect is deliberately excluded to prevent cheating.
}
