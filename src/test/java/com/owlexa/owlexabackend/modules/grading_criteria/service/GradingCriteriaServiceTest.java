package com.owlexa.owlexabackend.modules.grading_criteria.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradingCriteriaServiceTest {

    @Mock private GradingCriteriaRepository gradingCriteriaRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private QuestionRepository questionRepository;

    private GradingCriteriaService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long CRITERIA_ID = 30L;

    private User teacher;

    @BeforeEach
    void setUp() {
        service = new GradingCriteriaService(
                gradingCriteriaRepository,
                centerRepository,
                membershipRepository,
                authorizationService,
                questionRepository
        );

        TenantContext.setCurrentTenantId(CENTER_ID);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);

        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("delete: criteria used by active essay question throws BusinessRuleException")
    void delete_whenUsedByActiveEssayQuestion_shouldThrowBusinessRuleException() {
        GradingCriteria criteria = buildCriteria();
        when(gradingCriteriaRepository.findByIdAndCenter_IdAndDeletedAtIsNull(CRITERIA_ID, CENTER_ID))
                .thenReturn(Optional.of(criteria));
        when(questionRepository.existsByGradingCriteria_IdAndCenter_IdAndTypeAndDeletedAtIsNull(
                CRITERIA_ID,
                CENTER_ID,
                QuestionType.ESSAY
        )).thenReturn(true);

        assertThatThrownBy(() -> service.delete(CRITERIA_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể xóa tiêu chí chấm");
    }

    @Test
    @DisplayName("delete: criteria without active essay dependency is soft deleted")
    void delete_whenUnused_shouldSoftDeleteCriteria() {
        GradingCriteria criteria = buildCriteria();
        when(gradingCriteriaRepository.findByIdAndCenter_IdAndDeletedAtIsNull(CRITERIA_ID, CENTER_ID))
                .thenReturn(Optional.of(criteria));
        when(questionRepository.existsByGradingCriteria_IdAndCenter_IdAndTypeAndDeletedAtIsNull(
                CRITERIA_ID,
                CENTER_ID,
                QuestionType.ESSAY
        )).thenReturn(false);
        when(gradingCriteriaRepository.save(criteria)).thenReturn(criteria);

        service.delete(CRITERIA_ID);

        assertThat(criteria.getDeletedAt()).isNotNull();
        assertThat(criteria.getUpdatedBy()).isEqualTo(teacher);
        verify(gradingCriteriaRepository).save(criteria);
    }

    private GradingCriteria buildCriteria() {
        Center center = new Center();
        center.setId(CENTER_ID);

        return GradingCriteria.builder()
                .id(CRITERIA_ID)
                .center(center)
                .name("Writing Rubric")
                .content("Criteria content")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();
    }
}
