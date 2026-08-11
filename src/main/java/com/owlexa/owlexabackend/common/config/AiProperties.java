package com.owlexa.owlexabackend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "owlexa.ai")
public class AiProperties {

    /**
     * Master switch. Set to false to disable AI scoring entirely.
     */
    private boolean enabled = true;

    /**
     * AI provider to use.
     */
    private String provider = "deepseek";

    /** API key for the AI provider API. */
    private String apiKey = "";

    /** Model name, e.g. deepseek-chat. */
    private String model = "deepseek-chat";

    /** Base URL for the AI provider API. */
    private String baseUrl = "https://api.deepseek.com";

    /** HTTP timeout in milliseconds for the AI API call. */
    private int timeoutMs = 30000;

    /** Maximum number of retry attempts on transient failures. */
    private int maxRetries = 3;
}
