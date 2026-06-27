package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MockTestSubmitRequest {
    @NotNull
    private Long testId;

    @Valid
    private List<MockTestSubmitAnswerRequest> answers;
}
