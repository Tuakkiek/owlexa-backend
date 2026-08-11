package com.owlexa.owlexabackend.modules.ai_grading.provider.openai;

import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingCriterionOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingImprovementOutput;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class OpenAIGradingProvider implements AIGradingProvider {

    private static final String RESPONSES_PATH = "/v1/responses";
    private static final int RETRY_MAX_TOKENS_CAP = 16000;
    private static final int RETRY_MAX_TOKENS_INCREMENT = 2000;

    private final AIGradingProperties properties;
    private final ObjectMapper objectMapper;
    private final OpenAIGradingResultParser resultParser;
    private final RestClient restClient;

    public OpenAIGradingProvider(
            AIGradingProperties properties,
            ObjectMapper objectMapper,
            OpenAIGradingResultParser resultParser
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resultParser = resultParser;

        Duration timeout = Duration.ofMillis(properties.getTimeoutMs());
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AIModelProvider provider() {
        return AIModelProvider.OPENAI;
    }

    @Override
    public AIGradingProviderResponse grade(AIGradingProviderRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AIGradingProviderException("OpenAI API key is not configured");
        }

        return gradeInternal(request, true);
    }

    private String resolveEndpointPath() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl != null && (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/"))) {
            return "/chat/completions";
        }
        return "/v1/chat/completions";
    }

    private AIGradingProviderResponse gradeInternal(AIGradingProviderRequest request, boolean allowRetry) {
        String requestBody = buildRequestBody(request);
        String endpointPath = resolveEndpointPath();
        log.info(
                "AI grading HTTP request prepared: provider={}, baseUrl={}, endpoint={}, model={}, maxTokens={}, temperature={}, systemPromptLength={}, userPromptLength={}, payloadLength={}",
                provider(),
                properties.getBaseUrl(),
                endpointPath,
                request.modelName(),
                request.maxTokens(),
                request.temperature(),
                request.systemPrompt().length(),
                request.userPrompt().length(),
                requestBody.length()
        );

        try {
            AtomicInteger responseStatus = new AtomicInteger();
            String rawResponse = restClient.post()
                    .uri(endpointPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.setBearerAuth(properties.getApiKey());
                        headers.set(HttpHeaders.ACCEPT_ENCODING, "identity");
                    })
                    .body(requestBody)
                    .exchange((clientRequest, clientResponse) -> {
                        responseStatus.set(clientResponse.getStatusCode().value());
                        String responseBody;
                        try {
                            responseBody = StreamUtils.copyToString(
                                    clientResponse.getBody(),
                                    StandardCharsets.UTF_8
                            );
                        } catch (IOException exception) {
                            throw new RestClientException("Unable to read AI provider response body", exception);
                        }
                        return responseBody;
                    });

            if (responseStatus.get() >= 400) {
                log.warn(
                        "AI grading HTTP status failure: provider={}, model={}, status={}, responsePreview={}",
                        provider(),
                        request.modelName(),
                        responseStatus.get(),
                        preview(rawResponse)
                );
                throw new AIGradingProviderException(
                        "AI provider request failed with status " + responseStatus.get()
                );
            }

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new AIGradingProviderException("AI provider returned an empty response");
            }

            log.info(
                    "AI grading HTTP response received: provider={}, model={}, rawResponseLength={}, responsePreview={}",
                    provider(),
                    request.modelName(),
                    rawResponse.length(),
                    preview(rawResponse)
            );

            try {
                AIGradingOutput output = resultParser.parse(rawResponse);
                return new AIGradingProviderResponse(output, rawResponse);
            } catch (AIGradingProviderException exception) {
                if (allowRetry && isRetryableMaxOutputFailure(rawResponse)) {
                    AIGradingProviderRequest retriedRequest = expandMaxTokens(request);
                    if (retriedRequest.maxTokens() != null && !retriedRequest.maxTokens().equals(request.maxTokens())) {
                        log.warn(
                                "AI grading response was truncated; retrying once with a larger output budget: provider={}, model={}, originalMaxTokens={}, retryMaxTokens={}",
                                provider(),
                                request.modelName(),
                                request.maxTokens(),
                                retriedRequest.maxTokens()
                        );
                        return gradeInternal(retriedRequest, false);
                    }
                }

                log.warn(
                        "AI grading provider-level failure: provider={}, model={}, error={}",
                        provider(),
                        request.modelName(),
                        exception.getMessage()
                );
                throw exception;
            }
        } catch (RestClientException exception) {
            log.warn(
                    "AI grading transport failure: provider={}, model={}, error={}",
                    provider(),
                    request.modelName(),
                    exception.getMessage(),
                    exception
            );
            throw new AIGradingProviderException("AI provider request failed", exception);
        }
    }

    private String buildRequestBody(AIGradingProviderRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.modelName());
        root.put("max_tokens", request.maxTokens());
        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }

        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", request.systemPrompt());
        messages.addObject()
                .put("role", "user")
                .put("content", request.userPrompt());

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JacksonException exception) {
            throw new AIGradingProviderException("Unable to serialize AI grading request", exception);
        }
    }

    private ObjectNode resultSchema() {
        ObjectNode itemProperties = objectMapper.createObjectNode();
        itemProperties.set("itemNumber", integerSchema(1));
        itemProperties.set("aiScore", numberSchema(0, null));
        itemProperties.set("feedback", stringSchema());
        itemProperties.set("rubricAnalysis", stringSchema());
        itemProperties.set("confidence", numberSchema(0, 1));

        ObjectNode itemSchema = objectMapper.createObjectNode();
        itemSchema.put("type", "object");
        itemSchema.set("properties", itemProperties);
        itemSchema.set("required", requiredArray(
                "itemNumber",
                "aiScore",
                "feedback",
                "rubricAnalysis",
                "confidence"
        ));
        itemSchema.put("additionalProperties", false);

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.set("summary", stringSchema());
        propertiesNode.set("overallFeedback", stringSchema());
        propertiesNode.set("focusArea", stringSchema());
        propertiesNode.set("confidence", numberSchema(0, 1));

        ObjectNode criterionProperties = objectMapper.createObjectNode();
        criterionProperties.set("name", stringSchema());
        criterionProperties.set("score", numberSchema(0, null));
        criterionProperties.set("maxScore", numberSchema(0, null));
        criterionProperties.set("feedback", stringSchema());

        ObjectNode criterionSchema = objectMapper.createObjectNode();
        criterionSchema.put("type", "object");
        criterionSchema.set("properties", criterionProperties);
        criterionSchema.set("required", requiredArray("name", "score", "maxScore", "feedback"));
        criterionSchema.put("additionalProperties", false);

        ObjectNode criteriaArraySchema = objectMapper.createObjectNode();
        criteriaArraySchema.put("type", "array");
        criteriaArraySchema.set("items", criterionSchema);
        propertiesNode.set("criteria", criteriaArraySchema);

        ObjectNode improvementProperties = objectMapper.createObjectNode();
        improvementProperties.set("category", stringSchema());
        improvementProperties.set("issue", stringSchema());
        improvementProperties.set("suggestion", stringSchema());
        improvementProperties.set("example", stringSchema());

        ObjectNode improvementSchema = objectMapper.createObjectNode();
        improvementSchema.put("type", "object");
        improvementSchema.set("properties", improvementProperties);
        improvementSchema.set("required", requiredArray("category", "issue", "suggestion", "example"));
        improvementSchema.put("additionalProperties", false);

        ObjectNode improvementsArraySchema = objectMapper.createObjectNode();
        improvementsArraySchema.put("type", "array");
        improvementsArraySchema.set("items", improvementSchema);
        propertiesNode.set("improvements", improvementsArraySchema);

        ObjectNode itemsArraySchema = objectMapper.createObjectNode();
        itemsArraySchema.put("type", "array");
        itemsArraySchema.set("items", itemSchema);
        propertiesNode.set("items", itemsArraySchema);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", propertiesNode);
        schema.set("required", requiredArray(
                "summary",
                "overallFeedback",
                "focusArea",
                "confidence",
                "criteria",
                "improvements",
                "items"
        ));
        schema.put("additionalProperties", false);
        return schema;
    }

    private ObjectNode stringSchema() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode integerSchema(int minimum) {
        return objectMapper.createObjectNode()
                .put("type", "integer")
                .put("minimum", minimum);
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

    private ArrayNode requiredArray(String... names) {
        ArrayNode required = objectMapper.createArrayNode();
        for (String name : names) {
            required.add(name);
        }
        return required;
    }

    private boolean isRetryableMaxOutputFailure(String rawResponse) {
        try {
            ObjectNode response = (ObjectNode) objectMapper.readTree(rawResponse);
            return "incomplete".equals(response.path("status").asText())
                    && "max_output_tokens".equals(response.path("incomplete_details").path("reason").asText());
        } catch (JacksonException exception) {
            return false;
        }
    }

    private AIGradingProviderRequest expandMaxTokens(AIGradingProviderRequest request) {
        if (request.maxTokens() == null || request.maxTokens() <= 0) {
            return request;
        }

        int expandedMaxTokens = Math.min(
                RETRY_MAX_TOKENS_CAP,
                Math.max(request.maxTokens() * 2, request.maxTokens() + RETRY_MAX_TOKENS_INCREMENT)
        );
        if (expandedMaxTokens <= request.maxTokens()) {
            return request;
        }

        return new AIGradingProviderRequest(
                request.modelName(),
                request.temperature(),
                expandedMaxTokens,
                request.systemPrompt(),
                request.userPrompt()
        );
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }
}
