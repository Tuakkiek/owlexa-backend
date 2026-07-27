package com.owlexa.owlexabackend.modules.ai_grading.prompt;

public record AIGradingPromptSnapshot(
        String promptTemplateVersion,
        String promptBuilderVersion,
        String systemPrompt,
        String userPrompt
) {
}
