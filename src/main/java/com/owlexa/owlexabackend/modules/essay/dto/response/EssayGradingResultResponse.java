package com.owlexa.owlexabackend.modules.essay.dto.response;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class EssayGradingResultResponse {
    private Long id;
    private Long submissionId;
    private Double totalScore;
    private Double maxScore;
    private List<CriteriaScoreResponse> criteriaScores;
    private String feedback;
    private Instant gradedAt;

    @Data
    @Builder
    public static class CriteriaScoreResponse {
        private Long criteriaId;
        private String criteriaName;
        private Double score;
        private Double maxScore;
        private String feedback;
    }
}
