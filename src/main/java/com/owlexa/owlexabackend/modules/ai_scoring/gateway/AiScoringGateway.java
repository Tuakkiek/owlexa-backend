package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiScoringResponseDto;

/**
 * Gateway interface for AI scoring providers.
 */
public interface AiScoringGateway {

    /**
     * Sends the scoring prompt to the configured AI provider and returns the structured evaluation response.
     *
     * @param prompt formatted text containing essay and rubric criteria
     * @return provider-agnostic response dto containing scores and token metrics
     */
    AiScoringResponseDto scoreEssay(String prompt);
}
