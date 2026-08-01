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
    @DisplayName("parser: parses DeepSeek/OpenAI Chat Completions choices format successfully")
    void parse_whenChatCompletionsResponse_shouldReturnStructuredOutput() {
        String rawResponse = """
                {
                  "id": "chatcmpl-123",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\":\\"Excellent essay\\",\\"overallFeedback\\":\\"Good vocabulary\\",\\"confidence\\":0.95,\\"items\\":[{\\"itemNumber\\":1,\\"aiScore\\":6.0,\\"feedback\\":\\"Well structured\\",\\"rubricAnalysis\\":\\"Meets criteria\\",\\"confidence\\":0.9}]}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        AIGradingOutput output = parser.parse(rawResponse);

        assertThat(output.summary()).isEqualTo("Excellent essay");
        assertThat(output.confidence()).isEqualByComparingTo("0.95");
        assertThat(output.items()).hasSize(1);
        assertThat(output.items().get(0).aiScore()).isEqualByComparingTo("6.0");
    }

    @Test
    @DisplayName("parser: parses DeepSeek output with array rubricAnalysis and criterionScores flexibly")
    void parse_whenDeepSeekReturnsArrayRubricAnalysis_shouldParseFlexibly() {
        String rawResponse = """
                {
                  "id": "chatcmpl-456",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\":\\"Good essay\\",\\"criterionScores\\":[{\\"criterionName\\":\\"Ngữ pháp\\",\\"score\\":5.5,\\"maxScore\\":7.0}],\\"items\\":[{\\"itemNumber\\":1,\\"aiScore\\":5.5,\\"feedback\\":\\"Good effort\\",\\"rubricAnalysis\\":[\\"Point 1\\",\\"Point 2\\"]}]}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        AIGradingOutput output = parser.parse(rawResponse);

        assertThat(output.summary()).isEqualTo("Good essay");
        assertThat(output.criteria()).hasSize(1);
        assertThat(output.criteria().get(0).name()).isEqualTo("Ngữ pháp");
        assertThat(output.items()).hasSize(1);
        assertThat(output.items().get(0).rubricAnalysis()).contains("Point 1");
        assertThat(output.items().get(0).rubricAnalysis()).contains("Point 2");
    }

    @Test
    @DisplayName("parser: derives missing top-level fields from item-level criteria")
    void parse_whenDeepSeekNestsCriteriaUnderItems_shouldNormalizeOutput() {
        String rawResponse = """
                {
                  "id": "chatcmpl-789",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"items\\":[{\\"itemNumber\\":1,\\"criteria\\":[{\\"name\\":\\"Task response\\",\\"score\\":2.0,\\"maxScore\\":3.0,\\"feedback\\":\\"Clear position\\"},{\\"name\\":\\"Grammar\\",\\"score\\":1.5,\\"maxScore\\":2.0,\\"feedback\\":\\"Some sentence errors\\"}],\\"feedback\\":\\"Good effort with clear opinion\\",\\"analysis\\":\\"The essay answers the topic but needs stronger examples.\\"}]}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;

        AIGradingOutput output = parser.parse(rawResponse);

        assertThat(output.summary()).isEqualTo("Good effort with clear opinion");
        assertThat(output.overallFeedback()).contains("stronger examples");
        assertThat(output.focusArea()).isEqualTo("Task response");
        assertThat(output.confidence()).isEqualByComparingTo("0.7000");
        assertThat(output.criteria()).hasSize(2);
        assertThat(output.improvements()).hasSize(1);
        assertThat(output.items().get(0).aiScore()).isEqualByComparingTo("3.5");
        assertThat(output.items().get(0).confidence()).isEqualByComparingTo("0.7000");
    }

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
    @DisplayName("parser: incomplete response with valid structured output is accepted")
    void parse_whenResponseIsIncompleteButContainsValidJson_shouldReturnStructuredOutput() {
        String rawResponse = """
                {
                  "status": "incomplete",
                  "incomplete_details": {"reason": "max_output_tokens"},
                  "output": [
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
        assertThat(output.items()).hasSize(1);
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
