package com.owlexa.owlexabackend.modules.ai_grading.provider.openai;

import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OpenAIGradingResultParser {

    private final ObjectMapper objectMapper;

    public AIGradingOutput parse(String rawResponse) {
        try {
            JsonNode response = objectMapper.readTree(rawResponse);
            if (!"completed".equals(response.path("status").asText())) {
                throw new AIGradingProviderException("AI provider returned an incomplete grading response");
            }

            StringBuilder outputText = new StringBuilder();
            for (JsonNode output : response.path("output")) {
                if (!"message".equals(output.path("type").asText())) {
                    continue;
                }
                for (JsonNode content : output.path("content")) {
                    String type = content.path("type").asText();
                    if ("refusal".equals(type)) {
                        throw new AIGradingProviderException("AI provider refused the grading request");
                    }
                    if ("output_text".equals(type)) {
                        outputText.append(content.path("text").asText());
                    }
                }
            }

            if (outputText.isEmpty()) {
                throw new AIGradingProviderException("AI provider returned no grading result");
            }

            return objectMapper.readValue(outputText.toString(), AIGradingOutput.class);
        } catch (AIGradingProviderException exception) {
            throw exception;
        } catch (JacksonException exception) {
            throw new AIGradingProviderException("AI provider returned an invalid grading response", exception);
        }
    }
}
