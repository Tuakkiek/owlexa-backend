package com.owlexa.owlexabackend.modules.mocktest.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockTestQuestionResponse {
    private Long id;
    private Long testId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private String explanation;
    private Integer sortOrder;
}
