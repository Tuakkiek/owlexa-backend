package com.owlexa.owlexabackend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "owlexa.ai")
public class AiProperties {

    /**
     * Master switch. Set to false to disable AI scoring entirely (safe for
     * environments without an API key).
     */
    private boolean enabled = true;

    /**
     * AI provider to use: gemini | openai | mock.
     * Defaults to "mock" so development works without a live API key.
     */
    private String provider = "mock";

    /** API key for the chosen provider. */
    private String apiKey = "";

    /** Model name, e.g. gemini-2.0-flash or gpt-4o-mini. */
    private String model = "gemini-2.0-flash";

    /** HTTP timeout in milliseconds for the AI API call. */
    private int timeoutMs = 15000;

    /** Maximum number of retry attempts on transient failures. */
    private int maxRetries = 3;
}
