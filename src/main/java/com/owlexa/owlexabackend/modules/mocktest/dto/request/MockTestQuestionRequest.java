package com.owlexa.owlexabackend.modules.mocktest.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class MockTestQuestionRequest {
    @NotBlank
    private String questionText;

    @NotBlank
    private String optionA;

    @NotBlank
    private String optionB;

    @NotBlank
    private String optionC;

    @NotBlank
    private String optionD;

    @NotBlank
    @Pattern(regexp = "^[ABCD]$")
    private String correctAnswer;

    private String explanation;

    @PositiveOrZero
    private Integer sortOrder;
}
