package com.owlexa.owlexabackend.modules.mocktest.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MockTestSubmitAnswerRequest {
    @NotNull
    private Long questionId;

    @Pattern(regexp = "^[ABCD]$")
    private String answer;
}
