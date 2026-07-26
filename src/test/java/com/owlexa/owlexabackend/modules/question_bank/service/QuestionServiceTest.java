package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionOptionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
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

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private GradingCriteriaRepository gradingCriteriaRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;

    private QuestionService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long QUESTION_ID = 30L;
    private static final Long CRITERIA_ID = 40L;

    private User teacher;
    private Center center;

    @BeforeEach
    void setUp() {
        service = new QuestionService(
                questionRepository,
                gradingCriteriaRepository,
                centerRepository,
                membershipRepository,
                authorizationService
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
    @DisplayName("create: valid multiple choice creates question with options")
    void create_whenValidMultipleChoice_shouldCreateQuestion() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(QUESTION_ID);
            return question;
        });

        QuestionResponse response = service.create(validMultipleChoiceRequest());

        assertThat(response.getId()).isEqualTo(QUESTION_ID);
        assertThat(response.getType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions()).extracting("displayOrder").containsExactly(1, 2);
        assertThat(response.getOptions()).extracting("isCorrect").containsExactly(false, true);
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
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(QUESTION_ID);
            return question;
        });

        QuestionResponse response = service.create(validEssayRequest(CRITERIA_ID));

        assertThat(response.getType()).isEqualTo(QuestionType.ESSAY);
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
        when(questionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(question), pageable, 1));

        Page<QuestionResponse> response = service.findAll(
                "capital",
                QuestionType.MULTIPLE_CHOICE,
                QuestionDifficulty.EASY,
                null,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getOptions()).isNull();
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
        when(questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(QUESTION_ID, CENTER_ID))
                .thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionRequest request = validMultipleChoiceRequest();
        request.setOptions(List.of(
                option("New A", true, 1),
                option("New B", true, 2),
                option("New C", false, 3)
        ));

        QuestionResponse response = service.update(QUESTION_ID, request);

        assertThat(response.getOptions()).hasSize(3);
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
                .type(QuestionType.MULTIPLE_CHOICE)
                .title("Capital city")
                .content("What is the capital of Vietnam?")
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
                .type(QuestionType.ESSAY)
                .title("Opinion essay")
                .content("Do you agree or disagree?")
                .difficulty(QuestionDifficulty.MEDIUM)
                .points(BigDecimal.TEN)
                .gradingCriteriaId(gradingCriteriaId)
                .sampleAnswer("Sample answer")
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
                .type(QuestionType.MULTIPLE_CHOICE)
                .title("Capital city")
                .content("What is the capital of Vietnam?")
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
                .content("Criteria content")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();
    }
}
