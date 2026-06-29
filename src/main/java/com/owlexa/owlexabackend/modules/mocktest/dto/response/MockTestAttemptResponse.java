package com.owlexa.owlexabackend.modules.mocktest.dto.response;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class MockTestAttemptResponse {
    private Long id;
    private Long studentId;
    private String studentFullName;
    private Long testId;
    private String testTitle;
    private Integer durationMinutes;
    private Integer score;
    private Integer maxScore;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Instant startedAt;
    private Instant completedAt;
    private MockTestAttemptStatus status;
    private List<MockTestAttemptAnswerResponse> answers;
}
