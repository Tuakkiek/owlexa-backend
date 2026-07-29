package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;

record AIGradingExecutionContext(
        Long jobId,
        boolean shouldExecute,
        AIModelProvider provider,
        AIGradingProviderRequest providerRequest
) {
}
