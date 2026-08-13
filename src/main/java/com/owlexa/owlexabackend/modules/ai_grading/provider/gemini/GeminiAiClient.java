package com.owlexa.owlexabackend.modules.ai_grading.provider.gemini;

import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class GeminiAiClient implements AIGradingProvider {

    private final AIGradingProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiGradingResultParser resultParser;
    private final RestClient restClient;

    @Autowired
    public GeminiAiClient(
            AIGradingProperties properties,
            ObjectMapper objectMapper,
            GeminiGradingResultParser resultParser
    ) {
        this(properties, objectMapper, resultParser, RestClient.builder(), true);
    }

    GeminiAiClient(
            AIGradingProperties properties,
            ObjectMapper objectMapper,
            GeminiGradingResultParser resultParser,
            RestClient.Builder restClientBuilder
    ) {
        this(properties, objectMapper, resultParser, restClientBuilder, false);
    }

    private GeminiAiClient(
            AIGradingProperties properties,
            ObjectMapper objectMapper,
            GeminiGradingResultParser resultParser,
            RestClient.Builder restClientBuilder,
            boolean configureTransport
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resultParser = resultParser;

        RestClient.Builder configuredBuilder = restClientBuilder.baseUrl(properties.getBaseUrl());
        if (configureTransport) {
            Duration timeout = Duration.ofMillis(properties.getTimeoutMs());
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(timeout);
            configuredBuilder.requestFactory(requestFactory);
        }
        this.restClient = configuredBuilder.build();
    }

    @Override
    public AIModelProvider provider() {
        return AIModelProvider.GEMINI;
    }

    @Override
    public AIGradingProviderResponse grade(AIGradingProviderRequest request) {
        if (!isConfiguredApiKey(properties.getApiKey())) {
            throw new AIGradingProviderException(
                    "Gemini API key is not configured. Set a real GEMINI_API_KEY in .env."
            );
        }

        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                AIGradingProviderResponse response = requestGeminiWithTimeout(request);
                log.info(
                        "AI grading provider call completed: provider=GEMINI, model={}, durationMs={}, status=success",
                        request.modelName(), elapsedMillis(startedAt)
                );
                return response;
            } catch (AIGradingProviderException exception) {
                boolean retry = exception.retryable() && attempt < maxAttempts;
                log.warn(
                        "AI grading provider call failed: provider=GEMINI, model={}, durationMs={}, status={}, attempt={}, retry={}, error={}",
                        request.modelName(),
                        elapsedMillis(startedAt),
                        exception.statusCode() == 0 ? "transport" : exception.statusCode(),
                        attempt,
                        retry,
                        exception.getMessage()
                );
                if (!retry) {
                    throw exception;
                }
                backoff(attempt);
            }
        }
        throw new AIGradingProviderException("Gemini grading failed after retries");
    }

    private boolean isConfiguredApiKey(String apiKey) {
        return apiKey != null
                && !apiKey.isBlank()
                && !"your_gemini_api_key_here".equalsIgnoreCase(apiKey.trim());
    }

    private AIGradingProviderResponse requestGemini(AIGradingProviderRequest request) {
        String requestBody = buildRequestBody(request);
        AtomicInteger responseStatus = new AtomicInteger();
        try {
            String rawResponse = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .build(request.modelName()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", properties.getApiKey())
                    .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                    .body(requestBody)
                    .exchange((clientRequest, clientResponse) -> {
                        responseStatus.set(clientResponse.getStatusCode().value());
                        try {
                            return StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8);
                        } catch (IOException exception) {
                            throw new RestClientException("Unable to read Gemini response body", exception);
                        }
                    });

            int status = responseStatus.get();
            if (status >= 400) {
                throw providerException(status, rawResponse);
            }
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new AIGradingProviderException("Gemini returned an empty response");
            }

            AIGradingOutput output = resultParser.parse(rawResponse);
            return new AIGradingProviderResponse(output, rawResponse);
        } catch (AIGradingProviderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            if (isTimeout(exception)) {
                throw new AIGradingProviderException(
                        "Gemini grading timed out. Please try again.", exception, 0, true
                );
            }
            throw new AIGradingProviderException("Gemini grading request failed", exception, 0, true);
        }
    }

    private AIGradingProviderResponse requestGeminiWithTimeout(AIGradingProviderRequest request) {
        CompletableFuture<AIGradingProviderResponse> future = CompletableFuture.supplyAsync(
                () -> requestGemini(request)
        );
        try {
            return future.get(properties.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new AIGradingProviderException(
                    "Gemini grading timed out. Please try again.", exception, 0, true
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AIGradingProviderException(
                    "Gemini grading request was interrupted", exception, 0, true
            );
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AIGradingProviderException providerException) {
                throw providerException;
            }
            throw new AIGradingProviderException("Gemini grading request failed", cause, 0, true);
        }
    }

    private String buildRequestBody(AIGradingProviderRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode systemInstruction = root.putObject("systemInstruction");
        systemInstruction.putArray("parts").addObject().put("text", request.systemPrompt());

        ArrayNode contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject().put("role", "user");
        userContent.putArray("parts").addObject().put("text", request.userPrompt());

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", resultSchema());
        if (request.maxTokens() != null) {
            generationConfig.put("maxOutputTokens", request.maxTokens());
        }
        if (request.temperature() != null) {
            generationConfig.put("temperature", request.temperature());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JacksonException exception) {
            throw new AIGradingProviderException("Unable to serialize Gemini grading request", exception);
        }
    }

    private ObjectNode resultSchema() {
        ObjectNode root = objectMapper.createObjectNode().put("type", "object");
        ObjectNode properties = root.putObject("properties");
        properties.set("summary", stringSchema());
        properties.set("overallFeedback", stringSchema());
        properties.set("focusArea", stringSchema());
        properties.set("confidence", numberSchema(0, 1));

        ObjectNode criterion = objectSchema(
                new String[]{"name", "score", "maxScore", "feedback"},
                new ObjectNode[]{stringSchema(), numberSchema(0, null), numberSchema(0, null), stringSchema()}
        );
        ObjectNode criteria = properties.putObject("criteria").put("type", "array");
        criteria.set("items", criterion);

        ObjectNode improvement = objectSchema(
                new String[]{"category", "issue", "suggestion", "example"},
                new ObjectNode[]{stringSchema(), stringSchema(), stringSchema(), stringSchema()}
        );
        ObjectNode improvements = properties.putObject("improvements").put("type", "array");
        improvements.set("items", improvement);

        ObjectNode item = objectSchema(
                new String[]{"itemNumber", "aiScore", "feedback", "rubricAnalysis", "confidence"},
                new ObjectNode[]{integerSchema(1), numberSchema(0, null), stringSchema(), stringSchema(), numberSchema(0, 1)}
        );
        ObjectNode items = properties.putObject("items").put("type", "array");
        items.set("items", item);

        ArrayNode required = root.putArray("required");
        required.add("summary").add("overallFeedback").add("focusArea").add("confidence")
                .add("criteria").add("improvements").add("items");
        return root;
    }

    private ObjectNode objectSchema(String[] names, ObjectNode[] schemas) {
        ObjectNode object = objectMapper.createObjectNode().put("type", "object");
        ObjectNode properties = object.putObject("properties");
        ArrayNode required = object.putArray("required");
        for (int index = 0; index < names.length; index++) {
            properties.set(names[index], schemas[index]);
            required.add(names[index]);
        }
        return object;
    }

    private ObjectNode stringSchema() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode integerSchema(int minimum) {
        return objectMapper.createObjectNode().put("type", "integer").put("minimum", minimum);
    }

    private ObjectNode numberSchema(Integer minimum, Integer maximum) {
        ObjectNode schema = objectMapper.createObjectNode().put("type", "number");
        if (minimum != null) {
            schema.put("minimum", minimum);
        }
        if (maximum != null) {
            schema.put("maximum", maximum);
        }
        return schema;
    }

    private AIGradingProviderException providerException(int status, String responseBody) {
        if (status == 429) {
            return new AIGradingProviderException(
                    "AI grading is temporarily unavailable because the Gemini quota has been reached."
                            + providerDetail(responseBody),
                    status,
                    true
            );
        }
        if (status == 401 || status == 403) {
            return new AIGradingProviderException(
                    "Gemini API authentication failed. Check GEMINI_API_KEY."
                            + providerDetail(responseBody),
                    status,
                    false
            );
        }
        if (status >= 500) {
            return new AIGradingProviderException(
                    "Gemini is temporarily unavailable. Please try again."
                            + providerDetail(responseBody),
                    status,
                    true
            );
        }
        return new AIGradingProviderException(
                "Gemini rejected the grading request."
                        + providerDetail(responseBody),
                status,
                false
        );
    }

    private String providerDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        String normalized = responseBody.replaceAll("\\s+", " ").trim();
        int maxLength = 500;
        return " Gemini response: "
                + (normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...");
    }

    private void backoff(int attempt) {
        long delay = Math.min(2000L, Math.max(0L, properties.getRetryBackoffMs()) * (1L << (attempt - 1)));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AIGradingProviderException("Gemini grading retry was interrupted", exception);
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException
                    || current instanceof java.util.concurrent.TimeoutException
                    || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
