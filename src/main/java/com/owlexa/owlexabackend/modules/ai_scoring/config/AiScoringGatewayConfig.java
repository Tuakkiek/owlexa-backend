package com.owlexa.owlexabackend.modules.ai_scoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.AiScoringGateway;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.GeminiAiScoringGateway;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.MockAiScoringGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Conditionally wires the correct {@link AiScoringGateway} bean based on
 * {@code owlexa.ai.provider} configuration.
 *
 * <p>Supported providers:
 * <ul>
 *   <li>{@code mock} — no HTTP calls, deterministic results (default)</li>
 *   <li>{@code gemini} — Google Gemini REST API</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiScoringGatewayConfig {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public AiScoringGateway aiScoringGateway() {
        if (!aiProperties.isEnabled()) {
            log.info("[AI Scoring] AI scoring is DISABLED. Using no-op (mock) gateway.");
            return new MockAiScoringGateway();
        }

        String provider = aiProperties.getProvider();
        log.info("[AI Scoring] Initializing AI gateway for provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "gemini" -> {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(aiProperties.getTimeoutMs());
                factory.setReadTimeout(aiProperties.getTimeoutMs());
                RestTemplate restTemplate = new RestTemplate(factory);
                yield new GeminiAiScoringGateway(aiProperties, restTemplate, objectMapper);
            }
            case "mock" -> new MockAiScoringGateway();
            default -> {
                log.warn("[AI Scoring] Unknown provider '{}'. Falling back to mock.", provider);
                yield new MockAiScoringGateway();
            }
        };
    }
}
