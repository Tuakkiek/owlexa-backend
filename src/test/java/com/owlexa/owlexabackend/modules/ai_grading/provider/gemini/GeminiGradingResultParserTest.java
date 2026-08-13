package com.owlexa.owlexabackend.modules.ai_grading.provider.gemini;

import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiGradingResultParserTest {

    private final GeminiGradingResultParser parser = new GeminiGradingResultParser(new ObjectMapper());

    @Test
    void parse_whenGeminiReturnsStructuredCandidate_shouldReturnOutput() {
        String outputJson = "{\"summary\":\"Tốt\",\"overallFeedback\":\"Rõ ràng\",\"focusArea\":\"Ngữ pháp\",\"confidence\":0.9,\"criteria\":[],\"improvements\":[],\"items\":[]}";
        String rawResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + new ObjectMapper().writeValueAsString(outputJson)
                + "}]}}]}";

        AIGradingOutput output = parser.parse(rawResponse);

        assertThat(output.summary()).isEqualTo("Tốt");
        assertThat(output.confidence()).isEqualByComparingTo("0.9");
    }

    @Test
    void parse_whenResponseIsEmpty_shouldReject() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void parse_whenResponseContainsMalformedJson_shouldReject() {
        String rawResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"not-json\"}]}}]}";

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("malformed");
    }

    @Test
    void parse_whenGeminiBlocksThePrompt_shouldReject() {
        String rawResponse = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("blocked");
    }
}
