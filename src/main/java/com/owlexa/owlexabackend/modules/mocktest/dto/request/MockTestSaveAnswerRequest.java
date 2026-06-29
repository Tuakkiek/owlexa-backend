package com.owlexa.owlexabackend.modules.mocktest.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MockTestSaveAnswerRequest {
    @NotBlank
    @Pattern(regexp = "^[ABCD]$")
    private String answer;
}
