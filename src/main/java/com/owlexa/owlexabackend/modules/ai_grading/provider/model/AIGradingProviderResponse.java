package com.owlexa.owlexabackend.modules.ai_grading.provider.model;

public record AIGradingProviderResponse(
        AIGradingOutput output,
        String rawResponse
) {
}
