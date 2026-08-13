package com.owlexa.owlexabackend.modules.ai_grading.service;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingResultParser;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIGradingOutputReader {

    private final AIGradingResultParser resultParser;

    public Optional<AIGradingOutput> read(AIGradingResult result) {
        if (result == null || result.getRawResponse() == null || result.getRawResponse().isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(resultParser.parse(result.getRawResponse()));
        } catch (RuntimeException exception) {
            log.warn(
                    "Unable to extract structured AI grading insights from stored raw response: resultId={}, error={}",
                    result.getId(),
                    exception.getMessage()
            );
            return Optional.empty();
        }
    }
}
