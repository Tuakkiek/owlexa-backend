package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiCriterionResult;

/**
 * Port for external AI provider communication.
 * Implementations: {@link GeminiAiScoringGateway}, {@link MockAiScoringGateway}.
 */
public interface AiScoringGateway {

    /**
     * Sends the scoring prompt to the AI provider and returns a structured result.
     *
     * @param prompt The fully-constructed scoring prompt for a single rubric criterion.
     * @return Parsed {@link AiCriterionResult} with score and feedback.
     * @throws AiScoringException on provider errors that exhaust all retries.
     */
    AiCriterionResult scoreCriterion(String prompt);
}
