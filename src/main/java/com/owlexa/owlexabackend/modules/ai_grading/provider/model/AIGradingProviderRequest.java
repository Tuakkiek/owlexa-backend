package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

import java.math.BigDecimal;

public record AIGradingProviderRequest(
        String modelName,
        BigDecimal temperature,
        Integer maxTokens,
        String systemPrompt,
        String userPrompt
) {
}
