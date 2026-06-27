package com.owlexa.owlexabackend.dto.request;

import com.owlexa.owlexabackend.entity.MockTestLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MockTestRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private MockTestLevel level;

    @Min(1)
    private Integer duration;

    @Min(1)
    private Integer totalQuestions;

    private Boolean isActive;
}
