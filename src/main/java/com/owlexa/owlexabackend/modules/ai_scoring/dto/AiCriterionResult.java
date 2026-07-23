package com.owlexa.owlexabackend.modules.ai_scoring.dto;

/**
 * Parsed result from a single AI criterion scoring call.
 *
 * @param score    The score assigned by AI (between 0 and criterion.maxScore).
 * @param feedback One sentence of feedback in the same language as the student's answer.
 */
public record AiCriterionResult(Double score, String feedback) {
}
