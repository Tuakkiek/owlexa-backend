package com.owlexa.owlexabackend.modules.ai_grading.provider.gemini;

import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingResultParser;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiGradingResultParser implements AIGradingResultParser {

    private final ObjectMapper objectMapper;

    @Override
    public AIGradingOutput parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AIGradingProviderException("Gemini returned an empty grading response");
        }

        try {
            JsonNode response = objectMapper.readTree(rawResponse);
            String jsonText = extractText(response);
            if (jsonText.isBlank()) {
                throw new AIGradingProviderException("Gemini returned no grading content");
            }
            AIGradingOutput output = objectMapper.readValue(jsonText, AIGradingOutput.class);
            if (output == null) {
                throw new AIGradingProviderException("Gemini returned an empty grading result");
            }
            return output;
        } catch (AIGradingProviderException exception) {
            throw exception;
        } catch (JacksonException exception) {
            log.warn("Gemini grading response was not valid JSON: error={}", exception.getMessage());
            throw new AIGradingProviderException("Gemini returned malformed grading JSON", exception);
        }
    }

    private String extractText(JsonNode response) {
        if (response.has("summary")) {
            return response.toString();
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = response.path("promptFeedback").path("blockReason").asText("");
            throw new AIGradingProviderException(
                    blockReason.isBlank()
                            ? "Gemini returned no grading candidate"
                            : "Gemini blocked the grading prompt: " + blockReason
            );
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode candidate : candidates) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                if (part.has("text")) {
                    text.append(part.path("text").asText());
                }
            }
        }
        return text.toString().trim();
    }
}
