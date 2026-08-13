package com.owlexa.owlexabackend.modules.ai_grading.provider.gemini;

import com.owlexa.owlexabackend.modules.ai_grading.config.AIGradingProperties;
import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingProviderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiAiClientTest {

    private AIGradingProperties properties;
    private GeminiAiClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new AIGradingProperties();
        properties.setApiKey("test-key");
        properties.setModel("gemini-test");
        properties.setMaxRetries(2);
        properties.setRetryBackoffMs(0);
        var builder = org.springframework.web.client.RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiAiClient(properties, new ObjectMapper(), new GeminiGradingResultParser(new ObjectMapper()), builder);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void grade_whenGeminiReturnsSuccess_shouldSendStructuredRequest() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("responseMimeType"),
                        org.hamcrest.Matchers.containsString("responseSchema"),
                        org.hamcrest.Matchers.containsString("systemInstruction"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("additionalProperties"))
                )))
                .andRespond(withStatus(HttpStatus.OK).body(geminiResponse()));

        var response = client.grade(new AIGradingProviderRequest(
                "test-model", null, 800, "system", "user"
        ));

        assertThat(response.output().summary()).isEqualTo("Good");
    }

    @Test
    void grade_whenQuotaIsExhausted_shouldUseBoundedRetryAndFriendlyError() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{}"));

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("quota has been reached");
    }

    @Test
    void grade_whenAuthenticationFails_shouldNotRetry() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{}"));

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("authentication failed");
    }

    @Test
    void grade_whenGeminiRejectsRequest_shouldIncludeProviderResponseForDiagnosis() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":\"invalid schema\"}}"));

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("Gemini rejected the grading request")
                .hasMessageContaining("invalid schema")
                .satisfies(exception -> assertThat(((AIGradingProviderException) exception).statusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void grade_whenProviderReturns503_shouldRetryThenFailClearly() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{}"));

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("temporarily unavailable");
    }

    @Test
    void grade_whenRequestTimesOut_shouldRetryThenFailClearly() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent"))
                .andRespond(withException(new IOException("timeout", new HttpTimeoutException("timeout"))));

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void grade_whenApiKeyIsPlaceholder_shouldFailBeforeCallingGemini() {
        properties.setApiKey("your_gemini_api_key_here");

        assertThatThrownBy(() -> client.grade(request()))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("Set a real GEMINI_API_KEY");
    }

    private AIGradingProviderRequest request() {
        return new AIGradingProviderRequest("test-model", null, 800, "system", "user");
    }

    private String geminiResponse() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"summary\\\":\\\"Good\\\",\\\"overallFeedback\\\":\\\"Clear\\\",\\\"focusArea\\\":\\\"Grammar\\\",\\\"confidence\\\":0.9,\\\"criteria\\\":[],\\\"improvements\\\":[],\\\"items\\\":[]}\"}]}}]}";
    }
}
