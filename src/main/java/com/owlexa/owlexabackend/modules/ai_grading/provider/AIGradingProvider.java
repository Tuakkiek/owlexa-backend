package com.owlexa.owlexabackend.modules.ai_grading.provider;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderResponse;

public interface AIGradingProvider {

    AIModelProvider provider();

    AIGradingProviderResponse grade(AIGradingProviderRequest request);
}
