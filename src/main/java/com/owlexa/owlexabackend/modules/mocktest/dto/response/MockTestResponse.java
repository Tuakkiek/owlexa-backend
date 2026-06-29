package com.owlexa.owlexabackend.modules.mocktest.dto.response;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MockTestResponse {
    private Long id;
    private String title;
    private String description;
    private MockTestLevel level;
    private Integer duration;
    private Integer totalQuestions;
    private Instant createdAt;
    private Boolean isActive;
    private Integer questionCount;
    private Integer attemptCount;
}
