package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiCriterionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls the Google Gemini generateContent REST API to score a rubric criterion.
 *
 * <p>Request format (Gemini REST API v1beta):
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 *
 * <p>Response parsing: extracts {@code candidates[0].content.parts[0].text} and parses it as JSON.
 */
@Slf4j
@RequiredArgsConstructor
public class GeminiAiScoringGateway implements AiScoringGateway {

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AiCriterionResult scoreCriterion(String prompt) {
        String url = String.format(GEMINI_BASE_URL, aiProperties.getModel(), aiProperties.getApiKey());

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < aiProperties.getMaxRetries()) {
            attempt++;
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseGeminiResponse(response.getBody());
                }
                log.warn("[Gemini] Non-2xx response on attempt {}: {}", attempt, response.getStatusCode());

            } catch (Exception e) {
                lastException = e;
                log.warn("[Gemini] Attempt {} failed: {}", attempt, e.getMessage());
            }
        }

        throw new AiScoringException(
                "Gemini AI scoring failed after " + aiProperties.getMaxRetries() + " attempts.",
                lastException
        );
    }

    private AiCriterionResult parseGeminiResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            // The AI is instructed to return JSON only.
            JsonNode resultNode = objectMapper.readTree(text);
            double score = resultNode.path("score").asDouble(0.0);
            String feedback = resultNode.path("feedback").asText("No feedback provided.");

            return new AiCriterionResult(score, feedback);

        } catch (Exception e) {
            throw new AiScoringException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
