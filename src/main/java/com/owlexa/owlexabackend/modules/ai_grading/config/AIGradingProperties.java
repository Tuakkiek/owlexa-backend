package com.owlexa.owlexabackend.modules.ai_grading.config;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "owlexa.ai-grading")
public class AIGradingProperties {

    private boolean enabled = true;
    private AIModelProvider provider = AIModelProvider.GEMINI;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String apiKey = "";
    private String model = "gemini-2.5-flash";
    private BigDecimal temperature;
    private int maxTokens = 8000;
    private int timeoutMs = 120000;
    private int maxRetries = 3;
    private long retryBackoffMs = 500;
}
