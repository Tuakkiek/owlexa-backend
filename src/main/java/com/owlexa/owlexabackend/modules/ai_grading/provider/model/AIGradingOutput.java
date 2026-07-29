package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

import java.math.BigDecimal;
import java.util.List;

public record AIGradingOutput(
        String summary,
        String overallFeedback,
        BigDecimal confidence,
        List<AIGradingItemOutput> items
) {
}
