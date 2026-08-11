package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

import java.math.BigDecimal;

public record AIGradingCriterionOutput(
        String name,
        BigDecimal score,
        BigDecimal maxScore,
        String feedback
) {
}
