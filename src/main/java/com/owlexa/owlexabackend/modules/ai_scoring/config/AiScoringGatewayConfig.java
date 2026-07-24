package com.owlexa.owlexabackend.modules.ai_scoring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlexa.owlexabackend.common.config.AiProperties;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.AiScoringGateway;
import com.owlexa.owlexabackend.modules.ai_scoring.gateway.DeepSeekAiScoringGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Wires the {@link DeepSeekAiScoringGateway} bean for AI scoring.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiScoringGatewayConfig {

    private final AiProperties aiProperties;

    @Bean
    public DeepSeekAiScoringGateway deepSeekAiScoringGateway() {
        log.info("[AI Scoring] Initializing DeepSeek AI scoring gateway.");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getTimeoutMs());
        factory.setReadTimeout(aiProperties.getTimeoutMs());
        RestTemplate restTemplate = new RestTemplate(factory);
        return new DeepSeekAiScoringGateway(aiProperties, restTemplate, new ObjectMapper());
    }

    @Bean
    public AiScoringGateway aiScoringGateway(DeepSeekAiScoringGateway deepSeekAiScoringGateway) {
        return deepSeekAiScoringGateway;
    }
}
