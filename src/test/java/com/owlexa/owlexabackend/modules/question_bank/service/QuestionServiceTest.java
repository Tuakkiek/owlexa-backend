package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionOptionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.mapper.QuestionCollectionMapper;
import com.owlexa.owlexabackend.modules.question_bank.mapper.QuestionMapper;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.document;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.serializedDocument;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionCollectionRepository collectionRepository;
    @Mock private GradingCriteriaRepository gradingCriteriaRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private FileReferenceService fileReferenceService;

    private QuestionService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long QUESTION_ID = 30L;
    private static final Long CRITERIA_ID = 40L;

    private User teacher;
    private Center center;
    private QuestionCollection collection;

    @BeforeEach
    void setUp() {
        RichTextDocumentService richTextDocumentService =
                new RichTextDocumentService(new ObjectMapper());
        service = new QuestionService(
                questionRepository,
                collectionRepository,
                gradingCriteriaRepository,
                centerRepository,
                membershipRepository,
                authorizationService,
                richTextDocumentService,
                fileReferenceService,
                new QuestionMapper(richTextDocumentService, new QuestionCollectionMapper())
        );

        TenantContext.setCurrentTenantId(CENTER_ID);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);

        center = new Center();
        center.setId(CENTER_ID);
        collection = QuestionCollection.builder()
                .id(50L)
                .center(center)
                .code("TOEIC_TEST_1")
                .name("TOEIC Test 1")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();

        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
        lenient().when(collectionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(50L, CENTER_ID))
                .thenReturn(Optional.of(collection));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create: valid multiple choice creates question with options")
    void create_whenValidMultipleChoice_shouldCreateQuestion() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.saveAndFlush(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            assertThat(question.getQuestionCode()).startsWith("TMP-");
            question.setId(QUESTION_ID);
            return question;
        });

        QuestionResponse response = service.create(validMultipleChoiceRequest());

        assertThat(response.getId()).isEqualTo(QUESTION_ID);
        assertThat(response.getQuestionCode()).isEqualTo("Q-000030");
        assertThat(response.getType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getContent().path("type").asText()).isEqualTo("doc");
        assertThat(response.getContent().toString()).contains("capital of Vietnam");
        assertThat(response.getOptions()).extracting("displayOrder").containsExactly(1, 2);
        assertThat(response.getOptions()).extracting("isCorrect").containsExactly(false, true);
        verify(fileReferenceService).syncReferences(
                eq(FileOwnerType.QUESTION),
                eq(QUESTION_ID),
                eq(CENTER_ID),
                any()
        );
    }

    @Test
    @DisplayName("create: normalizes section code and stores collection metadata")
    void create_shouldNormalizeQuestionMetadata() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.saveAndFlush(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(QUESTION_ID);
            return question;
        });
        QuestionRequest request = validMultipleChoiceRequest();
        request.setSectionCode(" part_1 ");

        QuestionResponse response = service.create(request);

        assertThat(response.getCollection().getCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getSectionCode()).isEqualTo("PART_1");
        assertThat(response.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("create: duplicate display order in collection is rejected")
    void create_whenDisplayOrderExists_shouldThrowDuplicateResource() {
        when(questionRepository.existsByCollection_IdAndDisplayOrderAndDeletedAtIsNull(50L, 1))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(validMultipleChoiceRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Display order 1");
    }

    @Test
    @DisplayName("create: multiple choice with fewer than 2 options throws BadRequestException")
    void create_whenMultipleChoiceHasTooFewOptions_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.setOptions(List.of(option("A", true, 1)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: multiple choice without correct option throws BadRequestException")
    void create_whenMultipleChoiceHasNoCorrectOption_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.setOptions(List.of(
                option("A", false, 1),
                option("B", false, 2)
        ));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: multiple choice with grading criteria throws BadRequestException")
    void create_whenMultipleChoiceHasGradingCriteria_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.setGradingCriteriaId(CRITERIA_ID);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: question with zero points throws BadRequestException")
    void create_whenPointsAreZero_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.setPoints(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: question with negative points throws BadRequestException")
    void create_whenPointsAreNegative_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.setPoints(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: multiple choice option with null isCorrect throws BadRequestException")
    void create_whenOptionIsCorrectIsNull_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.getOptions().get(0).setIsCorrect(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: multiple choice option with null display order throws BadRequestException")
    void create_whenOptionDisplayOrderIsNull_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.getOptions().get(0).setDisplayOrder(null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: multiple choice option with invalid display order throws BadRequestException")
    void create_whenOptionDisplayOrderIsInvalid_shouldThrowBadRequest() {
        QuestionRequest request = validMultipleChoiceRequest();
        request.getOptions().get(0).setDisplayOrder(0);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: essay with criteria links active criteria in same center")
    void create_whenEssayHasActiveCriteria_shouldCreateQuestion() {
        GradingCriteria criteria = buildCriteria(CRITERIA_ID, "Writing Rubric");
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(gradingCriteriaRepository.findByIdAndCenter_IdAndDeletedAtIsNull(CRITERIA_ID, CENTER_ID))
                .thenReturn(Optional.of(criteria));
        when(questionRepository.saveAndFlush(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            assertThat(question.getQuestionCode()).startsWith("TMP-");
            question.setId(QUESTION_ID);
            return question;
        });

        QuestionResponse response = service.create(validEssayRequest(CRITERIA_ID));

        assertThat(response.getType()).isEqualTo(QuestionType.ESSAY);
        assertThat(response.getQuestionCode()).isEqualTo("Q-000030");
        assertThat(response.getGradingCriteria()).isNotNull();
        assertThat(response.getGradingCriteria().getId()).isEqualTo(CRITERIA_ID);
        assertThat(response.getOptions()).isEmpty();
    }

    @Test
    @DisplayName("create: essay with missing criteria throws ResourceNotFoundException")
    void create_whenEssayCriteriaMissing_shouldThrowResourceNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(gradingCriteriaRepository.findByIdAndCenter_IdAndDeletedAtIsNull(CRITERIA_ID, CENTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validEssayRequest(CRITERIA_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: essay with options throws BadRequestException")
    void create_whenEssayHasOptions_shouldThrowBadRequest() {
        QuestionRequest request = validEssayRequest(null);
        request.setOptions(List.of(option("A", true, 1), option("B", false, 2)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("findAll: returns paged question responses without option details")
    void findAll_shouldReturnPagedResponses() {
        Question question = buildMultipleChoiceQuestion();
        PageRequest pageable = PageRequest.of(0, 20);
        PageRequest effectivePageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.desc("updatedAt"))
        );
        when(questionRepository.findAll(any(Specification.class), eq(effectivePageable)))
                .thenReturn(new PageImpl<>(List.of(question), effectivePageable, 1));

        Page<QuestionResponse> response = service.findAll(
                "capital",
                null,
                null,
                QuestionType.MULTIPLE_CHOICE,
                QuestionDifficulty.EASY,
                null,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getOptions()).isNull();
    }

    @Test
    @DisplayName("findAll: rejects sort fields outside the public contract")
    void findAll_whenSortIsUnsupported_shouldThrowBadRequest() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("unknown"));

        assertThatThrownBy(() -> service.findAll(
                null,
                null,
                null,
                null,
                null,
                null,
                pageable
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported question sort");
    }

    @Test
    @DisplayName("findAll: collection picker defaults to display order")
    void findAll_whenCollectionIsSelected_shouldDefaultToDisplayOrder() {
        PageRequest requested = PageRequest.of(0, 20);
        PageRequest effective = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Order.asc("displayOrder"))
        );
        when(questionRepository.findAll(any(Specification.class), eq(effective)))
                .thenReturn(new PageImpl<>(List.of(), effective, 0));

        service.findAll(
                null,
                50L,
                null,
                null,
                null,
                null,
                requested
        );

        verify(questionRepository).findAll(any(Specification.class), eq(effective));
    }

    @Test
    @DisplayName("findSectionCodes: derives section order from the first question")
    void findSectionCodes_shouldDelegateForActiveTenantCollection() {
        when(questionRepository.findActiveSectionCodes(50L))
                .thenReturn(List.of("PART_1", "PART_2"));

        List<String> response = service.findSectionCodes(50L);

        assertThat(response).containsExactly("PART_1", "PART_2");
        verify(questionRepository).findActiveSectionCodes(50L);
    }

    @Test
    @DisplayName("validateImportBatch: duplicate display orders are rejected before persistence")
    void validateImportBatch_whenOrdersRepeat_shouldThrowDuplicateResource() {
        QuestionRequest first = validMultipleChoiceRequest();
        QuestionRequest second = validMultipleChoiceRequest();

        assertThatThrownBy(() -> service.validateImportBatch(List.of(first, second)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("duplicate display order");
    }

    @Test
    @DisplayName("findById: active question returns detail with sorted options")
    void findById_whenQuestionExists_shouldReturnDetail() {
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(buildMultipleChoiceQuestion()));

        QuestionResponse response = service.findById(QUESTION_ID);

        assertThat(response.getId()).isEqualTo(QUESTION_ID);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions()).extracting("displayOrder").containsExactly(1, 2);
    }

    @Test
    @DisplayName("update: replaces multiple choice options")
    void update_whenValid_shouldReplaceQuestionOptions() {
        Question existing = buildMultipleChoiceQuestion();
        String originalQuestionCode = existing.getQuestionCode();
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(existing));
        when(questionRepository.saveAndFlush(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuestionRequest request = validMultipleChoiceRequest();
        request.setOptions(List.of(
                option("New A", true, 1),
                option("New B", true, 2),
                option("New C", false, 3)
        ));

        QuestionResponse response = service.update(QUESTION_ID, request);

        assertThat(response.getOptions()).hasSize(3);
        assertThat(response.getQuestionCode()).isEqualTo(originalQuestionCode);
        assertThat(existing.getQuestionCode()).isEqualTo(originalQuestionCode);
        assertThat(existing.getOptions()).hasSize(3);
        assertThat(existing.getOptions()).extracting(QuestionOption::getContent)
                .containsExactly("New A", "New B", "New C");
    }

    @Test
    @DisplayName("delete: active question is soft deleted")
    void delete_whenQuestionExists_shouldSoftDelete() {
        Question existing = buildMultipleChoiceQuestion();
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(QUESTION_ID);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getUpdatedBy()).isEqualTo(teacher);
        verify(questionRepository).save(existing);
    }

    @Test
    @DisplayName("deleteMany: selected questions are soft deleted once")
    void deleteMany_whenQuestionsExist_shouldSoftDeleteDistinctQuestions() {
        Question first = buildMultipleChoiceQuestion();
        Question second = buildMultipleChoiceQuestion();
        second.setId(31L);
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(first));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(31L, CENTER_ID))
                .thenReturn(Optional.of(second));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteMany(Arrays.asList(QUESTION_ID, 31L, QUESTION_ID, null));

        assertThat(first.getDeletedAt()).isNotNull();
        assertThat(second.getDeletedAt()).isNotNull();
        assertThat(first.getUpdatedBy()).isEqualTo(teacher);
        assertThat(second.getUpdatedBy()).isEqualTo(teacher);
        verify(questionRepository).save(first);
        verify(questionRepository).save(second);
        verify(fileReferenceService, times(2)).syncReferences(
                eq(FileOwnerType.QUESTION),
                any(),
                eq(CENTER_ID),
                eq(List.of())
        );
    }

    @Test
    @DisplayName("create: non teacher throws AccessDeniedException")
    void create_whenUserIsNotTeacher_shouldThrowAccessDenied() {
        User owner = new User();
        owner.setId(99L);
        owner.setRole(Role.OWNER);
        when(authorizationService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.create(validMultipleChoiceRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create: teacher without center membership throws AccessDeniedException")
    void create_whenTeacherHasNoMembership_shouldThrowAccessDenied() {
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(validMultipleChoiceRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create: missing tenant context throws BadRequestException")
    void create_whenTenantContextMissing_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(validMultipleChoiceRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    private QuestionRequest validMultipleChoiceRequest() {
        return QuestionRequest.builder()
                .collectionId(50L)
                .sectionCode("PART_1")
                .displayOrder(1)
                .type(QuestionType.MULTIPLE_CHOICE)
                .content(document("What is the capital of Vietnam?"))
                .difficulty(QuestionDifficulty.EASY)
                .points(BigDecimal.ONE)
                .options(List.of(
                        option("Ho Chi Minh City", false, 1),
                        option("Ha Noi", true, 2)
                ))
                .build();
    }

    private QuestionRequest validEssayRequest(Long gradingCriteriaId) {
        return QuestionRequest.builder()
                .collectionId(50L)
                .sectionCode("WRITING")
                .displayOrder(1)
                .type(QuestionType.ESSAY)
                .content(document("Do you agree or disagree?"))
                .difficulty(QuestionDifficulty.MEDIUM)
                .points(BigDecimal.TEN)
                .gradingCriteriaId(gradingCriteriaId)
                .sampleAnswer(document("Sample answer"))
                .build();
    }

    private QuestionOptionRequest option(String content, boolean isCorrect, int displayOrder) {
        return QuestionOptionRequest.builder()
                .content(content)
                .isCorrect(isCorrect)
                .displayOrder(displayOrder)
                .build();
    }

    private Question buildMultipleChoiceQuestion() {
        Question question = Question.builder()
                .id(QUESTION_ID)
                .center(center)
                .collection(collection)
                .sectionCode("PART_1")
                .displayOrder(1)
                .type(QuestionType.MULTIPLE_CHOICE)
                .questionCode("Q-000030")
                .contentJson(serializedDocument("What is the capital of Vietnam?"))
                .difficulty(QuestionDifficulty.EASY)
                .points(BigDecimal.ONE)
                .createdBy(teacher)
                .updatedBy(teacher)
                .options(new ArrayList<>())
                .build();

        question.getOptions().add(QuestionOption.builder()
                .id(2L)
                .question(question)
                .content("Ha Noi")
                .isCorrect(true)
                .displayOrder(2)
                .build());
        question.getOptions().add(QuestionOption.builder()
                .id(1L)
                .question(question)
                .content("Ho Chi Minh City")
                .isCorrect(false)
                .displayOrder(1)
                .build());

        return question;
    }

    private GradingCriteria buildCriteria(Long id, String name) {
        return GradingCriteria.builder()
                .id(id)
                .center(center)
                .name(name)
                .contentJson(serializedDocument("Criteria content"))
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();
    }
}
