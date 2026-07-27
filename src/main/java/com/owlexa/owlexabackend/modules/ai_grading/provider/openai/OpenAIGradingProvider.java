package com.owlexa.owlexabackend.modules.ai_grading.provider.openai;

import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIModelProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProvider;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderResponse;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class OpenAIGradingProvider implements AIGradingProvider {

    private static final String RESPONSES_PATH = "/v1/responses";

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

        try {
            String rawResponse = restClient.post()
                    .uri(RESPONSES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(buildRequestBody(request))
                    .retrieve()
                    .body(String.class);

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new AIGradingProviderException("OpenAI returned an empty response");
            }

            AIGradingOutput output = resultParser.parse(rawResponse);
            return new AIGradingProviderResponse(output, rawResponse);
        } catch (AIGradingProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AIGradingProviderException(
                    "OpenAI request failed with status " + exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new AIGradingProviderException("OpenAI request failed", exception);
        }
    }

    private String buildRequestBody(AIGradingProviderRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.modelName());
        root.put("store", false);
        root.put("max_output_tokens", request.maxTokens());
        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }

        ArrayNode input = root.putArray("input");
        input.addObject()
                .put("role", "system")
                .put("content", request.systemPrompt());
        input.addObject()
                .put("role", "user")
                .put("content", request.userPrompt());

        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "essay_grading_result");
        format.put("strict", true);
        format.set("schema", resultSchema());

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JacksonException exception) {
            throw new AIGradingProviderException("Unable to serialize OpenAI grading request", exception);
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
        propertiesNode.set("confidence", numberSchema(0, 1));

        ObjectNode itemsArraySchema = objectMapper.createObjectNode();
        itemsArraySchema.put("type", "array");
        itemsArraySchema.set("items", itemSchema);
        propertiesNode.set("items", itemsArraySchema);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", propertiesNode);
        schema.set("required", requiredArray("summary", "overallFeedback", "confidence", "items"));
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
}
