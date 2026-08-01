package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

public record AIGradingImprovementOutput(
        String category,
        String issue,
        String suggestion,
        String example
) {
}
