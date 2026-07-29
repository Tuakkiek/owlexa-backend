package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportResultResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportValidationResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionImportServiceTest {

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;

    @Mock private QuestionService questionService;
    @Mock private QuestionCollectionService collectionService;
    @Mock private AuthorizationService authorizationService;
    @Mock private MembershipRepository membershipRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private QuestionImportService service;

    @BeforeEach
    void setUp() {
        service = new QuestionImportService(
                questionService,
                collectionService,
                objectMapper,
                authorizationService,
                membershipRepository
        );

        TenantContext.setCurrentTenantId(CENTER_ID);
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);
        when(authorizationService.getCurrentUser()).thenReturn(teacher);
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        lenient().when(collectionService.requireActiveById(50L))
                .thenReturn(QuestionCollection.builder()
                        .id(50L)
                        .code("TOEIC_TEST_1")
                        .name("TOEIC Test 1")
                        .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("validate: valid JSON returns preview without creating questions")
    void validate_whenPayloadIsValid_shouldReturnPreview() {
        QuestionImportValidationResponse response = service.validate(50L, validPayload());

        assertThat(response.getVersion()).isEqualTo("2.0");
        assertThat(response.getCollectionId()).isEqualTo(50L);
        assertThat(response.getCollectionName()).isEqualTo("TOEIC Test 1");
        assertThat(response.getCollectionCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getQuestionCount()).isEqualTo(1);
        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getSectionCode()).isEqualTo("PART_1");
        assertThat(response.getQuestions().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(response.getQuestions().get(0).getOptionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("importQuestions: converts JSON to QuestionRequest and delegates to QuestionService")
    void importQuestions_whenPayloadIsValid_shouldDelegateToQuestionService() {
        when(questionService.create(any(QuestionRequest.class))).thenReturn(QuestionResponse.builder()
                .id(1L)
                .questionCode("Q-000001")
                .type(QuestionType.MULTIPLE_CHOICE)
                .build());

        QuestionImportResultResponse response = service.importQuestions(50L, validPayload());

        assertThat(response.getImportedCount()).isEqualTo(1);
        assertThat(response.getQuestions()).extracting(QuestionResponse::getQuestionCode)
                .containsExactly("Q-000001");

        ArgumentCaptor<QuestionRequest> captor = ArgumentCaptor.forClass(QuestionRequest.class);
        verify(questionService).create(captor.capture());
        QuestionRequest request = captor.getValue();
        assertThat(request.getCollectionId()).isEqualTo(50L);
        assertThat(request.getSectionCode()).isEqualTo("PART_1");
        assertThat(request.getDisplayOrder()).isEqualTo(1);
        assertThat(request.getType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(request.getDifficulty()).isEqualTo(QuestionDifficulty.EASY);
        assertThat(request.getPoints()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(request.getContent().path("type").asText()).isEqualTo("doc");
        assertThat(request.getContent().toString()).contains("What is the capital of Vietnam?");
        assertThat(request.getOptions()).extracting("displayOrder").containsExactly(1, 2);
    }

    @Test
    @DisplayName("validate: multiple choice without correct option reports question number")
    void validate_whenNoCorrectOption_shouldThrowQuestionError() {
        String payload = """
                {
                  "version": "2.0",
                  "questions": [
                    {
                      "sectionCode": "PART_1",
                      "displayOrder": 1,
                      "type": "MULTIPLE_CHOICE",
                      "content": "Pick one",
                      "options": [
                        { "content": "A", "isCorrect": false },
                        { "content": "B", "isCorrect": false }
                      ]
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> service.validate(50L, payload))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Question 1: Exactly one correct option is required.");
    }

    @Test
    @DisplayName("validate: version 1 payload is intentionally unsupported")
    void validate_whenVersionIsOld_shouldRejectPayload() {
        String payload = """
                {
                  "version": "1.0",
                  "questions": []
                }
                """;

        assertThatThrownBy(() -> service.validate(50L, payload))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported import version: 1.0.");
    }

    @Test
    @DisplayName("validate: multiple choice may omit content for listening workflows")
    void validate_whenContentIsMissing_shouldReturnPreview() {
        String payload = """
                {
                  "version": "2.0",
                  "questions": [
                    {
                      "sectionCode": "PART_1",
                      "displayOrder": 1,
                      "type": "MULTIPLE_CHOICE",
                      "options": [
                        { "content": "A", "isCorrect": true },
                        { "content": "B", "isCorrect": false }
                      ]
                    }
                  ]
                }
                """;

        QuestionImportValidationResponse response = service.validate(50L, payload);

        assertThat(response.getQuestions().get(0).getContent()).isEmpty();
    }

    private String validPayload() {
        return """
                {
                  "version": "2.0",
                  "questions": [
                    {
                      "sectionCode": "PART_1",
                      "displayOrder": 1,
                      "type": "MULTIPLE_CHOICE",
                      "content": "What is the capital of Vietnam?",
                      "difficulty": "EASY",
                      "points": 1,
                      "ignored": "ok",
                      "options": [
                        { "content": "Ho Chi Minh City", "isCorrect": false },
                        { "content": "Ha Noi", "isCorrect": true }
                      ]
                    }
                  ]
                }
                """;
    }
}
