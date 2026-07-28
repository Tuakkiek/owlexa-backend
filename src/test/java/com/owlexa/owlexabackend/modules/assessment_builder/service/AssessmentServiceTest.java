package com.owlexa.owlexabackend.modules.assessment_builder.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentItemRequest;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentRequest;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentDetailResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentListResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.mapper.AssessmentMapper;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentRepository;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.databind.ObjectMapper;
import static com.owlexa.owlexabackend.support.RichTextTestFixtures.serializedDocument;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private StoredFileRepository storedFileRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private FileReferenceService fileReferenceService;

    private AssessmentService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long ASSESSMENT_ID = 30L;
    private static final Long QUESTION_ID = 40L;
    private static final Long ESSAY_QUESTION_ID = 41L;
    private static final Long CRITERIA_ID = 50L;

    private User teacher;
    private Center center;

    @BeforeEach
    void setUp() {
        RichTextDocumentService documentService = new RichTextDocumentService(new ObjectMapper());
        service = new AssessmentService(
                assessmentRepository,
                questionRepository,
                storedFileRepository,
                centerRepository,
                membershipRepository,
                authorizationService,
                new AssessmentMapper(documentService, new FileMapper()),
                documentService,
                fileReferenceService
        );

        TenantContext.setCurrentTenantId(CENTER_ID);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);

        center = new Center();
        center.setId(CENTER_ID);

        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create: valid request creates draft assessment with question snapshot")
    void create_whenValid_shouldCreateDraftAssessmentWithSnapshot() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(buildMultipleChoiceQuestion()));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> {
            Assessment assessment = invocation.getArgument(0);
            assessment.setId(ASSESSMENT_ID);
            return assessment;
        });

        AssessmentDetailResponse response = service.create(validAssessmentRequest(null));

        assertThat(response.getId()).isEqualTo(ASSESSMENT_ID);
        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.DRAFT);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(response.getItems().get(0).getPoints()).isEqualByComparingTo("2.50");
        assertThat(response.getItems().get(0).getOptions()).hasSize(2);
        assertThat(response.getContent().path("type").asText()).isEqualTo("doc");
        assertThat(response.getContent().toString()).contains("Short quiz");
        verify(fileReferenceService).syncReferences(
                eq(com.owlexa.owlexabackend.modules.file.entity.FileOwnerType.ASSESSMENT),
                eq(ASSESSMENT_ID),
                eq(CENTER_ID),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("create: item points override question points")
    void create_whenItemPointsProvided_shouldUseOverridePoints() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(buildMultipleChoiceQuestion()));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentDetailResponse response = service.create(validAssessmentRequest(new BigDecimal("5.00")));

        assertThat(response.getItems().get(0).getPoints()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("create: essay snapshots grading criteria summary and content")
    void create_whenEssayHasCriteria_shouldSnapshotCriteria() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ESSAY_QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(buildEssayQuestion()));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentRequest request = AssessmentRequest.builder()
                .title("Essay Exam")
                .type(AssessmentType.EXAM)
                .items(List.of(item(ESSAY_QUESTION_ID, null, 1)))
                .build();

        AssessmentDetailResponse response = service.create(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuestionType()).isEqualTo(QuestionType.ESSAY);
        assertThat(response.getItems().get(0).getGradingCriteriaId()).isEqualTo(CRITERIA_ID);
        assertThat(response.getItems().get(0).getGradingCriteriaName()).isEqualTo("Writing Rubric");
        assertThat(response.getItems().get(0).getGradingCriteriaContent().toString())
                .contains("Rubric content");
    }

    @Test
    @DisplayName("create: duplicate question id throws BadRequestException")
    void create_whenDuplicateQuestionIds_shouldThrowBadRequest() {
        AssessmentRequest request = AssessmentRequest.builder()
                .title("Quiz")
                .type(AssessmentType.QUIZ)
                .items(List.of(
                        item(QUESTION_ID, null, 1),
                        item(QUESTION_ID, null, 2)
                ))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: duplicate display order throws BadRequestException")
    void create_whenDuplicateDisplayOrders_shouldThrowBadRequest() {
        AssessmentRequest request = AssessmentRequest.builder()
                .title("Quiz")
                .type(AssessmentType.QUIZ)
                .items(List.of(
                        item(QUESTION_ID, null, 1),
                        item(ESSAY_QUESTION_ID, null, 1)
                ))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: missing question throws ResourceNotFoundException")
    void create_whenQuestionMissing_shouldThrowResourceNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validAssessmentRequest(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: replaces all items and preserves lifecycle status")
    void update_whenValid_shouldReplaceItemsAndKeepStatus() {
        Assessment existing = buildAssessment(AssessmentStatus.PUBLISHED, List.of(buildExistingItem(1L, 1)));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(existing));
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ESSAY_QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(buildEssayQuestion()));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentRequest request = AssessmentRequest.builder()
                .title("Updated Assessment")
                .type(AssessmentType.HOMEWORK)
                .items(List.of(item(ESSAY_QUESTION_ID, BigDecimal.TEN, 1)))
                .build();

        AssessmentDetailResponse response = service.update(ASSESSMENT_ID, request);

        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.PUBLISHED);
        assertThat(existing.getItems()).hasSize(1);
        assertThat(existing.getItems().get(0).getQuestion().getId()).isEqualTo(ESSAY_QUESTION_ID);
        assertThat(response.getItems().get(0).getQuestionType()).isEqualTo(QuestionType.ESSAY);
        assertThat(response.getItems().get(0).getPoints()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("publish: draft assessment with items becomes published")
    void publish_whenDraftAndValid_shouldPublish() {
        Assessment assessment = buildAssessment(AssessmentStatus.DRAFT, List.of(buildExistingItem(QUESTION_ID, 1)));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentDetailResponse response = service.publish(ASSESSMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.PUBLISHED);
        assertThat(assessment.getUpdatedBy()).isEqualTo(teacher);
    }

    @Test
    @DisplayName("publish: draft assessment without items throws BadRequestException")
    void publish_whenNoItems_shouldThrowBadRequest() {
        Assessment assessment = buildAssessment(AssessmentStatus.DRAFT, List.of());
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.publish(ASSESSMENT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("publish: already published assessment throws BadRequestException")
    void publish_whenAlreadyPublished_shouldThrowBadRequest() {
        Assessment assessment = buildAssessment(AssessmentStatus.PUBLISHED, List.of(buildExistingItem(QUESTION_ID, 1)));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.publish(ASSESSMENT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("archive: published assessment becomes archived")
    void archive_whenPublished_shouldArchive() {
        Assessment assessment = buildAssessment(AssessmentStatus.PUBLISHED, List.of(buildExistingItem(QUESTION_ID, 1)));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentDetailResponse response = service.archive(ASSESSMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssessmentStatus.ARCHIVED);
        assertThat(assessment.getUpdatedBy()).isEqualTo(teacher);
    }

    @Test
    @DisplayName("archive: draft assessment throws BadRequestException")
    void archive_whenDraft_shouldThrowBadRequest() {
        Assessment assessment = buildAssessment(AssessmentStatus.DRAFT, List.of(buildExistingItem(QUESTION_ID, 1)));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.archive(ASSESSMENT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("delete: active assessment is soft deleted")
    void delete_whenAssessmentExists_shouldSoftDelete() {
        Assessment assessment = buildAssessment(AssessmentStatus.DRAFT, List.of());
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(ASSESSMENT_ID);

        assertThat(assessment.getDeletedAt()).isNotNull();
        assertThat(assessment.getUpdatedBy()).isEqualTo(teacher);
        verify(assessmentRepository).save(assessment);
    }

    @Test
    @DisplayName("findAll: returns paged list responses")
    void findAll_shouldReturnPagedListResponses() {
        Assessment assessment = buildAssessment(AssessmentStatus.DRAFT, List.of());
        PageRequest pageable = PageRequest.of(0, 20);
        when(assessmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assessment), pageable, 1));

        Page<AssessmentListResponse> response = service.findAll(
                "quiz",
                AssessmentType.QUIZ,
                AssessmentStatus.DRAFT,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Assessment");
    }

    @Test
    @DisplayName("findById: missing or soft deleted assessment throws ResourceNotFoundException")
    void findById_whenAssessmentMissing_shouldThrowResourceNotFound() {
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ASSESSMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: non teacher throws AccessDeniedException")
    void create_whenUserIsNotTeacher_shouldThrowAccessDenied() {
        User owner = new User();
        owner.setId(99L);
        owner.setRole(Role.OWNER);
        when(authorizationService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.create(validAssessmentRequest(null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create: teacher without center membership throws AccessDeniedException")
    void create_whenTeacherHasNoMembership_shouldThrowAccessDenied() {
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(validAssessmentRequest(null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create: missing tenant context throws BadRequestException")
    void create_whenTenantContextMissing_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(validAssessmentRequest(null)))
                .isInstanceOf(BadRequestException.class);
    }

    private AssessmentRequest validAssessmentRequest(BigDecimal points) {
        return AssessmentRequest.builder()
                .title("Quiz")
                .description("Short quiz")
                .type(AssessmentType.QUIZ)
                .items(List.of(item(QUESTION_ID, points, 1)))
                .build();
    }

    private AssessmentItemRequest item(Long questionId, BigDecimal points, int displayOrder) {
        return AssessmentItemRequest.builder()
                .questionId(questionId)
                .points(points)
                .displayOrder(displayOrder)
                .build();
    }

    private Assessment buildAssessment(AssessmentStatus status, List<AssessmentItem> items) {
        Assessment assessment = Assessment.builder()
                .id(ASSESSMENT_ID)
                .center(center)
                .type(AssessmentType.QUIZ)
                .status(status)
                .title("Assessment")
                .description("Description")
                .contentJson("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Description\"}]}]}")
                .createdBy(teacher)
                .updatedBy(teacher)
                .items(new ArrayList<>())
                .build();

        items.forEach(item -> {
            item.setAssessment(assessment);
            assessment.getItems().add(item);
        });

        return assessment;
    }

    private AssessmentItem buildExistingItem(Long questionId, int displayOrder) {
        AssessmentItem item = AssessmentItem.builder()
                .id(questionId + 100L)
                .question(buildQuestion(questionId, QuestionType.MULTIPLE_CHOICE))
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Capital city")
                .contentJson(serializedDocument("What is the capital of Vietnam?"))
                .difficulty(QuestionDifficulty.EASY)
                .points(new BigDecimal("2.50"))
                .displayOrder(displayOrder)
                .options(new ArrayList<>())
                .build();

        item.getOptions().add(AssessmentItemOption.builder()
                .assessmentItem(item)
                .content("Ha Noi")
                .isCorrect(true)
                .displayOrder(1)
                .build());
        item.getOptions().add(AssessmentItemOption.builder()
                .assessmentItem(item)
                .content("Ho Chi Minh City")
                .isCorrect(false)
                .displayOrder(2)
                .build());

        return item;
    }

    private Question buildMultipleChoiceQuestion() {
        Question question = buildQuestion(QUESTION_ID, QuestionType.MULTIPLE_CHOICE);
        question.setContentJson(serializedDocument("What is the capital of Vietnam?"));
        question.setDifficulty(QuestionDifficulty.EASY);
        question.setPoints(new BigDecimal("2.50"));
        question.setOptions(new ArrayList<>());

        question.getOptions().add(QuestionOption.builder()
                .id(1L)
                .question(question)
                .content("Ha Noi")
                .isCorrect(true)
                .displayOrder(1)
                .build());
        question.getOptions().add(QuestionOption.builder()
                .id(2L)
                .question(question)
                .content("Ho Chi Minh City")
                .isCorrect(false)
                .displayOrder(2)
                .build());

        return question;
    }

    private Question buildEssayQuestion() {
        GradingCriteria criteria = GradingCriteria.builder()
                .id(CRITERIA_ID)
                .center(center)
                .name("Writing Rubric")
                .contentJson(serializedDocument("Rubric content"))
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();

        Question question = buildQuestion(ESSAY_QUESTION_ID, QuestionType.ESSAY);
        question.setContentJson(serializedDocument("Write an essay"));
        question.setDifficulty(QuestionDifficulty.MEDIUM);
        question.setPoints(BigDecimal.TEN);
        question.setGradingCriteria(criteria);
        question.setSampleAnswerJson(serializedDocument("Sample answer"));
        question.setOptions(new ArrayList<>());
        return question;
    }

    private Question buildQuestion(Long questionId, QuestionType type) {
        return Question.builder()
                .id(questionId)
                .center(center)
                .type(type)
                .contentJson(serializedDocument("Question content"))
                .difficulty(QuestionDifficulty.EASY)
                .points(BigDecimal.ONE)
                .createdBy(teacher)
                .updatedBy(teacher)
                .options(new ArrayList<>())
                .build();
    }
}
