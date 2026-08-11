package com.owlexa.owlexabackend.modules.ai_grading.provider.openai;

import com.owlexa.owlexabackend.modules.ai_grading.provider.AIGradingProviderException;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingCriterionOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingImprovementOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingItemOutput;
import com.owlexa.owlexabackend.modules.ai_grading.provider.model.AIGradingOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIGradingResultParser {

    private final ObjectMapper objectMapper;

    public AIGradingOutput parse(String rawResponse) {
        try {
            JsonNode response = objectMapper.readTree(rawResponse);
            String status = response.path("status").asText();
            StringBuilder outputText = new StringBuilder();

            // 1. Standard OpenAI / DeepSeek Chat Completions format ("choices")
            if (response.has("choices") && response.path("choices").isArray()) {
                for (JsonNode choice : response.path("choices")) {
                    JsonNode message = choice.path("message");
                    if (message.has("refusal") && !message.path("refusal").isNull() && !message.path("refusal").asText().isBlank()) {
                        throw new AIGradingProviderException("AI provider refused the grading request: " + message.path("refusal").asText());
                    }
                    if (message.has("content") && !message.path("content").isNull()) {
                        outputText.append(message.path("content").asText());
                    }
                }
            }

            // 2. OpenAI Responses format ("output")
            if (outputText.isEmpty() && response.has("output") && response.path("output").isArray()) {
                for (JsonNode output : response.path("output")) {
                    if (!"message".equals(output.path("type").asText())) {
                        continue;
                    }
                    for (JsonNode content : output.path("content")) {
                        String type = content.path("type").asText();
                        if ("refusal".equals(type)) {
                            throw new AIGradingProviderException("AI provider refused the grading request");
                        }
                        if ("output_text".equals(type)) {
                            outputText.append(content.path("text").asText());
                        }
                    }
                }
            }

            // 3. Fallback: direct JSON text if rawResponse is the output JSON itself
            if (outputText.isEmpty() && response.has("items")) {
                outputText.append(rawResponse);
            }

            if (outputText.isEmpty()) {
                if ("incomplete".equalsIgnoreCase(status)) {
                    throw new AIGradingProviderException("AI provider returned an incomplete grading response");
                }
                throw new AIGradingProviderException("AI provider returned no grading result");
            }

            String jsonText = outputText.toString().trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            AIGradingOutput parsed;
            try {
                parsed = objectMapper.readValue(jsonText, AIGradingOutput.class);
                if (needsFlexibleParsing(parsed)) {
                    log.info("AI grading JSON output is structurally incomplete; applying flexible parser");
                    parsed = parseFlexibleJson(jsonText);
                }
            } catch (JacksonException e) {
                log.info("Standard Jackson deserialization failed for LLM JSON output; applying flexible parser");
                parsed = parseFlexibleJson(jsonText);
            }

            log.info(
                    "AI grading response parsed successfully: summaryLength={}, itemCount={}",
                    parsed.summary() == null ? 0 : parsed.summary().length(),
                    parsed.items() == null ? 0 : parsed.items().size()
            );
            return parsed;
        } catch (AIGradingProviderException exception) {
            log.warn("AI grading response parsing failed: error={}, preview={}", exception.getMessage(), preview(rawResponse));
            throw exception;
        } catch (JacksonException exception) {
            log.warn(
                    "AI grading response JSON parsing failed: error={}, preview={}",
                    exception.getMessage(),
                    preview(rawResponse),
                    exception
            );
            throw new AIGradingProviderException("AI provider returned an invalid grading response", exception);
        }
    }

    private AIGradingOutput parseFlexibleJson(String jsonText) throws JacksonException {
        JsonNode root = objectMapper.readTree(jsonText);

        String summary = extractStringOrJoinArray(root, "summary");
        String overallFeedback = extractStringOrJoinArray(root, "overallFeedback");
        String focusArea = extractStringOrJoinArray(root, "focusArea");
        BigDecimal confidence = parseBigDecimal(root.path("confidence"));

        List<AIGradingCriterionOutput> criteria = new ArrayList<>();
        JsonNode criteriaNode = root.has("criteria") ? root.path("criteria") : root.path("criterionScores");
        if (criteriaNode.isArray()) {
            for (JsonNode c : criteriaNode) {
                criteria.add(parseCriterion(c));
            }
        } else if (root.path("items").isArray()) {
            for (JsonNode itemNode : root.path("items")) {
                JsonNode itemCriteriaNode = itemNode.has("criteria")
                        ? itemNode.path("criteria")
                        : itemNode.path("criterionScores");
                if (!itemCriteriaNode.isArray()) {
                    continue;
                }
                for (JsonNode c : itemCriteriaNode) {
                    criteria.add(parseCriterion(c));
                }
                break;
            }
        }

        List<AIGradingImprovementOutput> improvements = new ArrayList<>();
        if (root.path("improvements").isArray()) {
            for (JsonNode imp : root.path("improvements")) {
                improvements.add(new AIGradingImprovementOutput(
                        extractStringOrJoinArray(imp, "category"),
                        extractStringOrJoinArray(imp, "issue"),
                        extractStringOrJoinArray(imp, "suggestion"),
                        extractStringOrJoinArray(imp, "example")
                ));
            }
        }

        List<AIGradingItemOutput> items = new ArrayList<>();
        if (root.path("items").isArray()) {
            for (JsonNode itemNode : root.path("items")) {
                BigDecimal aiScore = parseBigDecimal(itemNode.path("aiScore"));
                if (aiScore == null) {
                    aiScore = sumCriterionScores(itemNode);
                }
                items.add(new AIGradingItemOutput(
                        itemNode.path("itemNumber").asInt(1),
                        aiScore,
                        firstText(itemNode, "feedback", "overallFeedback"),
                        firstText(itemNode, "rubricAnalysis", "analysis", "criteria"),
                        defaultConfidence(parseBigDecimal(itemNode.path("confidence")))
                ));
            }
        }

        if (summary == null && !items.isEmpty()) {
            summary = truncateText(items.get(0).feedback(), 300);
        }
        if (overallFeedback == null && !items.isEmpty()) {
            overallFeedback = truncateText(items.get(0).rubricAnalysis(), 1000);
        }
        if (focusArea == null && !criteria.isEmpty()) {
            focusArea = criteria.get(0).name();
        }
        if (confidence == null) {
            confidence = defaultConfidence(null);
        }
        if (improvements.isEmpty() && !criteria.isEmpty()) {
            AIGradingCriterionOutput criterion = criteria.get(0);
            improvements.add(new AIGradingImprovementOutput(
                    criterion.name(),
                    criterion.feedback(),
                    "Tập trung sửa lỗi trong phần này ở bài viết tiếp theo.",
                    "Viết lại một câu tiêu biểu rồi kiểm tra ngữ pháp, từ vựng và liên kết ý."
            ));
        }

        return new AIGradingOutput(
                summary,
                overallFeedback,
                focusArea,
                confidence,
                criteria,
                improvements,
                items
        );
    }

    private AIGradingCriterionOutput parseCriterion(JsonNode c) {
        String name = extractStringOrJoinArray(c, "name");
        if (name == null) {
            name = extractStringOrJoinArray(c, "criterionName");
        }
        return new AIGradingCriterionOutput(
                name,
                parseBigDecimal(c.has("score") ? c.path("score") : c.path("aiScore")),
                parseBigDecimal(c.path("maxScore")),
                extractStringOrJoinArray(c, "feedback")
        );
    }

    private boolean needsFlexibleParsing(AIGradingOutput output) {
        return output == null
                || isBlank(output.summary())
                || isBlank(output.overallFeedback())
                || isBlank(output.focusArea())
                || output.confidence() == null
                || output.criteria() == null
                || output.criteria().isEmpty()
                || output.improvements() == null
                || output.improvements().isEmpty()
                || output.items() == null
                || output.items().stream().anyMatch(item -> item == null
                        || item.aiScore() == null
                        || isBlank(item.feedback())
                        || isBlank(item.rubricAnalysis())
                        || item.confidence() == null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal sumCriterionScores(JsonNode itemNode) {
        JsonNode criteriaNode = itemNode.has("criteria")
                ? itemNode.path("criteria")
                : itemNode.path("criterionScores");
        if (!criteriaNode.isArray()) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean hasScore = false;
        for (JsonNode criterion : criteriaNode) {
            BigDecimal score = parseBigDecimal(criterion.has("score")
                    ? criterion.path("score")
                    : criterion.path("aiScore"));
            if (score != null) {
                total = total.add(score);
                hasScore = true;
            }
        }
        return hasScore ? total : null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = extractStringOrJoinArray(node, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal defaultConfidence(BigDecimal confidence) {
        return confidence == null ? new BigDecimal("0.7000") : confidence;
    }

    private String truncateText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String extractStringOrJoinArray(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return null;
        }
        if (field.isTextual()) {
            return field.asText();
        }
        if (field.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode elem : field) {
                if (sb.length() > 0) sb.append("\n");
                if (elem.isTextual()) {
                    sb.append(elem.asText());
                } else if (elem.isObject() && elem.has("criterionName")) {
                    sb.append(elem.path("criterionName").asText());
                    if (elem.has("feedback")) {
                        sb.append(": ").append(elem.path("feedback").asText());
                    }
                } else {
                    sb.append(elem.toString());
                }
            }
            return sb.toString();
        }
        return field.toString();
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return new BigDecimal(node.asText());
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText().trim());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }
}
