package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiBulkScoringResult;
import com.owlexa.owlexabackend.modules.ai_scoring.dto.AiScoringResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek HTTP implementation of the AI Scoring Gateway.
 */
@Slf4j
@RequiredArgsConstructor
public class DeepSeekAiScoringGateway implements AiScoringGateway {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AiScoringResponseDto scoreEssay(String prompt) {
        String baseUrl = aiProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com";
        }
        
        String url = baseUrl;
        if (!url.endsWith("/chat/completions")) {
            url = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
        }

        String model = aiProperties.getModel();
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are an experienced English teacher.\nYou must evaluate ONLY according to the provided rubric.\nReturn ONLY valid JSON."
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < aiProperties.getMaxRetries()) {
            attempt++;
            try {
                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                long latency = System.currentTimeMillis() - startTime;

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("[DeepSeekAiScoringGateway] AI scoring successful. Provider: {}, Model: {}, Latency: {}ms, Status: {}", 
                            aiProperties.getProvider(), model, latency, response.getStatusCode());
                    return parseAiResponse(response.getBody(), latency, model);
                }
                log.warn("[DeepSeekAiScoringGateway] Non-2xx response on attempt {}: {} - Latency: {}ms", attempt, response.getStatusCode(), latency);

            } catch (Exception e) {
                lastException = e;
                log.warn("[DeepSeekAiScoringGateway] Attempt {} failed: {}", attempt, e.getMessage());
            }

            if (attempt < aiProperties.getMaxRetries()) {
                long sleepTimeMs = (long) Math.pow(2, attempt - 1) * 1000;
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiScoringException("AI scoring interrupted during retry backoff.", ie);
                }
            }
        }

        throw new AiScoringException(
                "AI scoring failed after " + aiProperties.getMaxRetries() + " attempts.",
                lastException
        );
    }

    private AiScoringResponseDto parseAiResponse(String rawResponse, long latency, String model) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (text != null && text.contains("```json")) {
                text = text.substring(text.indexOf("```json") + 7, text.lastIndexOf("```"));
            } else if (text != null && text.contains("```")) {
                text = text.substring(text.indexOf("```") + 3, text.lastIndexOf("```"));
            }

            AiBulkScoringResult result = objectMapper.readValue(text, AiBulkScoringResult.class);
            
            Integer promptTokens = null;
            Integer responseTokens = null;
            Integer totalTokens = null;
            
            JsonNode usageNode = root.path("usage");
            if (!usageNode.isMissingNode()) {
                promptTokens = usageNode.path("prompt_tokens").isMissingNode() ? null : usageNode.path("prompt_tokens").asInt();
                responseTokens = usageNode.path("completion_tokens").isMissingNode() ? null : usageNode.path("completion_tokens").asInt();
                totalTokens = usageNode.path("total_tokens").isMissingNode() ? null : usageNode.path("total_tokens").asInt();
            }

            return new AiScoringResponseDto(result, promptTokens, responseTokens, totalTokens, latency, model);

        } catch (Exception e) {
            throw new AiScoringException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}
