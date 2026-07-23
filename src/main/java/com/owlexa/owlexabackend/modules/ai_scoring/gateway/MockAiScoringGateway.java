package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiCriterionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock implementation of {@link AiScoringGateway} for development and testing.
 * Returns deterministic results (75% of max score) without making any HTTP calls.
 *
 * <p>Active when {@code owlexa.ai.provider=mock} (the default).
 */
@Slf4j
public class MockAiScoringGateway implements AiScoringGateway {

    private static final double MOCK_SCORE_FRACTION = 0.75;
    private static final String MOCK_FEEDBACK =
            "Mock AI feedback: The student demonstrated a reasonable understanding of the criterion.";

    @Override
    public AiCriterionResult scoreCriterion(String prompt) {
        log.debug("[MockAI] scoreCriterion called — returning deterministic mock result.");
        // Extract maxScore hint from the prompt if present (format: "Maximum Score: X")
        double maxScore = extractMaxScore(prompt);
        double score = Math.round(maxScore * MOCK_SCORE_FRACTION * 100.0) / 100.0;
        return new AiCriterionResult(score, MOCK_FEEDBACK);
    }

    private double extractMaxScore(String prompt) {
        try {
            String marker = "Maximum Score: ";
            int idx = prompt.indexOf(marker);
            if (idx < 0) return 10.0; // sensible default
            int start = idx + marker.length();
            int end = prompt.indexOf('\n', start);
            String val = (end > start) ? prompt.substring(start, end).trim() : prompt.substring(start).trim();
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 10.0;
        }
    }
}
