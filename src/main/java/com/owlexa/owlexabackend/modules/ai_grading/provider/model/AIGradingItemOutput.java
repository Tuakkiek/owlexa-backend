package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

import java.math.BigDecimal;

public record AIGradingItemOutput(
        Integer itemNumber,
        BigDecimal aiScore,
        String feedback,
        String rubricAnalysis,
        BigDecimal confidence
) {
}
