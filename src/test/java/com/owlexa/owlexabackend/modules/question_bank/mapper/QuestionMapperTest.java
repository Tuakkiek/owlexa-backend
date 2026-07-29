package com.owlexa.owlexabackend.modules.question_bank.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionMapperTest {

    @Mock
    private RichTextDocumentService richTextDocumentService;

    private QuestionCollectionMapper collectionMapper;
    private QuestionMapper questionMapper;

    @BeforeEach
    void setUp() {
        collectionMapper = new QuestionCollectionMapper();
        questionMapper = new QuestionMapper(richTextDocumentService, collectionMapper);
    }

    @Test
    void mapsCollectionWithCallerSuppliedQuestionCount() {
        Instant createdAt = Instant.parse("2026-07-01T01:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-02T02:00:00Z");
        QuestionCollection collection = QuestionCollection.builder()
                .id(11L)
                .code("TOEIC_TEST_1")
                .name("TOEIC Test 1")
                .description("Listening and reading practice")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        QuestionCollectionResponse response = collectionMapper.toResponse(collection, 100L);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getName()).isEqualTo("TOEIC Test 1");
        assertThat(response.getDescription()).isEqualTo("Listening and reading practice");
        assertThat(response.getQuestionCount()).isEqualTo(100L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsQuestionMetadataAndKeepsOptionsOutOfListResponse() {
        Question question = question();
        JsonNode content = mock(JsonNode.class);
        JsonNode explanation = mock(JsonNode.class);
        JsonNode sampleAnswer = mock(JsonNode.class);
        when(richTextDocumentService.deserialize("{\"type\":\"doc\"}")).thenReturn(content);
        when(richTextDocumentService.deserializeOptional("explanation")).thenReturn(explanation);
        when(richTextDocumentService.deserializeOptional("sample")).thenReturn(sampleAnswer);

        QuestionResponse response = questionMapper.toListResponse(question);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getQuestionCode()).isEqualTo("Q-001");
        assertThat(response.getCollection().getId()).isEqualTo(11L);
        assertThat(response.getCollection().getCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getCollection().getName()).isEqualTo("TOEIC Test 1");
        assertThat(response.getSectionCode()).isEqualTo("PART_1");
        assertThat(response.getDisplayOrder()).isEqualTo(1);
        assertThat(response.getContent()).isSameAs(content);
        assertThat(response.getExplanation()).isSameAs(explanation);
        assertThat(response.getSampleAnswer()).isSameAs(sampleAnswer);
        assertThat(response.getOptions()).isNull();
        verify(richTextDocumentService).deserialize("{\"type\":\"doc\"}");
    }

    @Test
    void mapsDetailOptionsInDisplayOrder() {
        Question question = question();
        question.setOptions(new ArrayList<>(List.of(
                option(32L, "Second", false, 2),
                option(31L, "First", true, 1)
        )));
        when(richTextDocumentService.deserialize("{\"type\":\"doc\"}"))
                .thenReturn(mock(JsonNode.class));

        QuestionResponse response = questionMapper.toDetailResponse(question);

        assertThat(response.getOptions())
                .extracting(option -> option.getDisplayOrder())
                .containsExactly(1, 2);
        assertThat(response.getOptions())
                .extracting(option -> option.getContent())
                .containsExactly("First", "Second");
    }

    private Question question() {
        QuestionCollection collection = QuestionCollection.builder()
                .id(11L)
                .code("TOEIC_TEST_1")
                .name("TOEIC Test 1")
                .build();

        return Question.builder()
                .id(21L)
                .questionCode("Q-001")
                .collection(collection)
                .sectionCode("PART_1")
                .displayOrder(1)
                .type(QuestionType.MULTIPLE_CHOICE)
                .contentJson("{\"type\":\"doc\"}")
                .difficulty(QuestionDifficulty.EASY)
                .points(new BigDecimal("1.00"))
                .explanationJson("explanation")
                .sampleAnswerJson("sample")
                .createdAt(Instant.parse("2026-07-01T01:00:00Z"))
                .updatedAt(Instant.parse("2026-07-02T02:00:00Z"))
                .build();
    }

    private QuestionOption option(Long id, String content, boolean correct, int displayOrder) {
        return QuestionOption.builder()
                .id(id)
                .content(content)
                .isCorrect(correct)
                .displayOrder(displayOrder)
                .build();
    }
}
