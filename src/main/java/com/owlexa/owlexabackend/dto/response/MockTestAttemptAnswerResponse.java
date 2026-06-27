package com.owlexa.owlexabackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockTestAttemptAnswerResponse {
    private Long questionId;
    private String questionText;
    private String studentAnswer;
    private Boolean isCorrect;
    private String correctAnswer;
}
