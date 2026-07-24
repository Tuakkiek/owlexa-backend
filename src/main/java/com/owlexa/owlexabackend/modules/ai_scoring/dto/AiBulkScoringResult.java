package com.owlexa.owlexabackend.modules.ai_scoring.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiBulkScoringResult {
    private List<CriterionResult> criteria;
    private String overallFeedback;
    private String improvementSuggestions;

    @Data
    public static class CriterionResult {
        private String criterion;
        private Double score;
        private String feedback;
    }
}
