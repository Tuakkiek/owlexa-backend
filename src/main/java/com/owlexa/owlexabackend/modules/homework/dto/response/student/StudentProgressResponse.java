package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentProgressResponse {
    private double averageScore;
    private int totalCompleted;
    private int totalMissing;
    private int totalLate;
    private List<ScoreHistoryItem> scoreHistory;

    @Data
    @Builder
    public static class ScoreHistoryItem {
        private Long assignmentId;
        private String assignmentTitle;
        private double score;
        private double maxScore;
        private java.time.Instant submittedAt;
    }
}
