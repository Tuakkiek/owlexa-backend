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
    private AIModelProvider provider = AIModelProvider.OPENAI;
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private BigDecimal temperature;
    private int maxTokens = 8000;
    private int timeoutMs = 120000;
}
