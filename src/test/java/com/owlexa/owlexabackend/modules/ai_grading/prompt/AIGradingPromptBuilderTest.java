package com.owlexa.owlexabackend.modules.ai_grading.prompt;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AIGradingPromptBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AIGradingPromptBuilder promptBuilder = new AIGradingPromptBuilder(objectMapper);

    @Test
    @DisplayName("prompt: contains only assignment snapshot grading data")
    void build_shouldUseSnapshotAndExcludeIdentityData() throws Exception {
        AssessmentItem liveAssessmentItem = AssessmentItem.builder()
                .id(900L)
                .content("Changed live assessment content")
                .build();
        Assignment assignment = Assignment.builder()
                .id(800L)
                .title("Private assignment title")
                .build();
        AssignmentItem snapshotItem = AssignmentItem.builder()
                .id(700L)
                .assignment(assignment)
                .assessmentItem(liveAssessmentItem)
                .questionType(QuestionType.ESSAY)
                .content("Snapshot question")
                .gradingCriteriaName("Snapshot rubric")
                .gradingCriteriaContent("Use evidence and clear reasoning")
                .points(new BigDecimal("5.00"))
                .displayOrder(1)
                .build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(600L)
                .assignmentItem(snapshotItem)
                .answerText("Snapshot student answer")
                .maxScore(new BigDecimal("5.00"))
                .build();

        AIGradingPromptSnapshot prompt = promptBuilder.build(List.of(answer));
        JsonNode userPrompt = objectMapper.readTree(prompt.userPrompt());
        JsonNode promptItem = userPrompt.path("items").get(0);

        assertThat(prompt.promptTemplateVersion()).isEqualTo("essay-grading-v1");
        assertThat(prompt.promptBuilderVersion()).isEqualTo("assignment-snapshot-v1");
        assertThat(promptItem.path("itemNumber").asInt()).isEqualTo(1);
        assertThat(promptItem.path("question").asText()).isEqualTo("Snapshot question");
        assertThat(promptItem.path("studentAnswer").asText()).isEqualTo("Snapshot student answer");
        assertThat(promptItem.path("rubricName").asText()).isEqualTo("Snapshot rubric");
        assertThat(promptItem.path("rubric").asText()).isEqualTo("Use evidence and clear reasoning");
        assertThat(promptItem.path("maximumScore").decimalValue()).isEqualByComparingTo("5.00");

        assertThat(prompt.userPrompt())
                .doesNotContain("Changed live assessment content")
                .doesNotContain("Private assignment title")
                .doesNotContain("studentUserId")
                .doesNotContain("email")
                .doesNotContain("phone")
                .doesNotContain("center")
                .doesNotContain("recipient")
                .doesNotContain("\"id\"");
    }

    @Test
    @DisplayName("prompt: item numbering follows the supplied snapshot order")
    void build_shouldAssignStableSequentialItemNumbers() throws Exception {
        SubmissionAnswer first = answer(1L, 1, "First question");
        SubmissionAnswer second = answer(2L, 2, "Second question");

        AIGradingPromptSnapshot prompt = promptBuilder.build(List.of(first, second));
        JsonNode items = objectMapper.readTree(prompt.userPrompt()).path("items");

        assertThat(items.get(0).path("itemNumber").asInt()).isEqualTo(1);
        assertThat(items.get(0).path("question").asText()).isEqualTo("First question");
        assertThat(items.get(1).path("itemNumber").asInt()).isEqualTo(2);
        assertThat(items.get(1).path("question").asText()).isEqualTo("Second question");
    }

    private SubmissionAnswer answer(Long id, int displayOrder, String content) {
        AssignmentItem item = AssignmentItem.builder()
                .id(id + 100)
                .questionType(QuestionType.ESSAY)
                .content(content)
                .points(new BigDecimal("5.00"))
                .displayOrder(displayOrder)
                .build();
        return SubmissionAnswer.builder()
                .id(id)
                .assignmentItem(item)
                .answerText("Answer " + id)
                .maxScore(new BigDecimal("5.00"))
                .build();
    }
}
