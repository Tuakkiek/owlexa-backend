package com.owlexa.owlexabackend.modules.ai_grading.provider;

import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;

public interface AIGradingResultParser {

    AIGradingOutput parse(String rawResponse);
}
