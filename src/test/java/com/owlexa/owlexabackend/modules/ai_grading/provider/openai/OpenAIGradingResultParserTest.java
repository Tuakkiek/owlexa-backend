package com.owlexa.owlexabackend.modules.ai_grading.provider.openai;

import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIGradingResultParserTest {

    private final OpenAIGradingResultParser parser = new OpenAIGradingResultParser(new ObjectMapper());

    @Test
    @DisplayName("parser: finds structured output after non-message response items")
    void parse_whenCompletedResponse_shouldReturnStructuredOutput() {
        String rawResponse = """
                {
                  "status": "completed",
                  "output": [
                    {"type": "reasoning", "id": "reasoning-1"},
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"summary\\":\\"Strong answer\\",\\"overallFeedback\\":\\"Add one example\\",\\"confidence\\":0.9,\\"items\\":[{\\"itemNumber\\":1,\\"aiScore\\":4.5,\\"feedback\\":\\"Clear\\",\\"rubricAnalysis\\":\\"Meets most criteria\\",\\"confidence\\":0.8}]}"
                        }
                      ]
                    }
                  ]
                }
                """;

        AIGradingOutput output = parser.parse(rawResponse);

        assertThat(output.summary()).isEqualTo("Strong answer");
        assertThat(output.confidence()).isEqualByComparingTo("0.9");
        assertThat(output.items()).hasSize(1);
        assertThat(output.items().get(0).aiScore()).isEqualByComparingTo("4.5");
    }

    @Test
    @DisplayName("parser: incomplete provider response is rejected")
    void parse_whenResponseIsIncomplete_shouldReject() {
        String rawResponse = """
                {"status":"incomplete","output":[]}
                """;

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("incomplete");
    }

    @Test
    @DisplayName("parser: provider refusal is rejected")
    void parse_whenProviderRefuses_shouldReject() {
        String rawResponse = """
                {
                  "status":"completed",
                  "output":[
                    {
                      "type":"message",
                      "content":[{"type":"refusal","refusal":"Unable to grade"}]
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("refused");
    }

    @Test
    @DisplayName("parser: malformed structured output is rejected")
    void parse_whenOutputTextIsMalformed_shouldReject() {
        String rawResponse = """
                {
                  "status":"completed",
                  "output":[
                    {
                      "type":"message",
                      "content":[{"type":"output_text","text":"not-json"}]
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> parser.parse(rawResponse))
                .isInstanceOf(AIGradingProviderException.class)
                .hasMessageContaining("invalid");
    }
}
