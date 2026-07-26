package com.owlexa.owlexabackend.modules.assignment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentRepository;
import com.owlexa.owlexabackend.modules.assignment.dto.request.AssignmentRequest;
import com.owlexa.owlexabackend.modules.assignment.dto.request.AssignmentTargetRequest;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTarget;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import com.owlexa.owlexabackend.modules.assignment.mapper.AssignmentMapper;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
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
import java.time.Instant;
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
class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentRecipientRepository assignmentRecipientRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;

    private AssignmentService service;

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long STUDENT_ID = 30L;
    private static final Long SECOND_STUDENT_ID = 31L;
    private static final Long ASSIGNMENT_ID = 40L;
    private static final Long ASSESSMENT_ID = 50L;
    private static final Long CLASS_ID = 60L;

    private Center center;
    private User teacher;
    private User student;
    private User secondStudent;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(
                assignmentRepository,
                assignmentRecipientRepository,
                assessmentRepository,
                classRepository,
                classEnrollmentRepository,
                userRepository,
                centerRepository,
                membershipRepository,
                authorizationService,
                new AssignmentMapper()
        );

        TenantContext.setCurrentTenantId(CENTER_ID);

        center = new Center();
        center.setId(CENTER_ID);

        teacher = user(TEACHER_ID, Role.TEACHER, "Teacher");
        student = user(STUDENT_ID, Role.STUDENT, "Student One");
        secondStudent = user(SECOND_STUDENT_ID, Role.STUDENT, "Student Two");

        lenient().when(authorizationService.getCurrentUser()).thenReturn(teacher);
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create: valid class target creates draft assignment without snapshot")
    void create_whenValidClassTarget_shouldCreateDraftWithoutSnapshot() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(buildAssessment(AssessmentStatus.PUBLISHED)));
        when(classRepository.findByIdAndCenter_Id(CLASS_ID, CENTER_ID)).thenReturn(Optional.of(buildClass(ClassStatus.ACTIVE)));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(ASSIGNMENT_ID);
            return assignment;
        });

        AssignmentDetailResponse response = service.create(validClassAssignmentRequest());

        assertThat(response.getId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(response.getAssessmentSnapshotAt()).isNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTargets()).hasSize(1);
        assertThat(response.getTargets().get(0).getClassId()).isEqualTo(CLASS_ID);
    }

    @Test
    @DisplayName("create: draft assessment cannot be assigned")
    void create_whenAssessmentNotPublished_shouldThrowBadRequest() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(buildAssessment(AssessmentStatus.DRAFT)));

        assertThatThrownBy(() -> service.create(validClassAssignmentRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: duplicate targets throw BadRequestException")
    void create_whenDuplicateTargets_shouldThrowBadRequest() {
        AssignmentRequest request = AssignmentRequest.builder()
                .assessmentId(ASSESSMENT_ID)
                .title("Homework")
                .targets(List.of(classTarget(CLASS_ID), classTarget(CLASS_ID)))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: invalid time window throws BadRequestException")
    void create_whenOpenAtIsNotBeforeDueAt_shouldThrowBadRequest() {
        Instant now = Instant.now();
        AssignmentRequest request = AssignmentRequest.builder()
                .assessmentId(ASSESSMENT_ID)
                .title("Homework")
                .openAt(now)
                .dueAt(now)
                .targets(List.of(classTarget(CLASS_ID)))
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: direct student target validates student role and center membership")
    void create_whenValidStudentTarget_shouldCreateDraftAssignment() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSESSMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(buildAssessment(AssessmentStatus.PUBLISHED)));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentDetailResponse response = service.create(validStudentAssignmentRequest());

        assertThat(response.getTargets()).hasSize(1);
        assertThat(response.getTargets().get(0).getStudentUserId()).isEqualTo(STUDENT_ID);
    }

    @Test
    @DisplayName("update: only draft assignments can be updated")
    void update_whenAssignmentIsNotDraft_shouldThrowBadRequest() {
        Assignment assignment = buildAssignment(AssignmentStatus.ACTIVE);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.update(ASSIGNMENT_ID, validClassAssignmentRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("publish: draft assignment snapshots assessment and materializes collapsed recipients")
    void publish_whenDraftWithMixedTargets_shouldSnapshotAndCreateRecipients() {
        Assignment assignment = buildDraftAssignmentWithTargets(List.of(
                buildClassTarget(),
                buildStudentTarget(student)
        ));
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(enrollment(student), enrollment(secondStudent)));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentDetailResponse response = service.publish(ASSIGNMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(response.getAssessmentSnapshotAt()).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getOptions()).hasSize(2);
        assertThat(response.getRecipients()).hasSize(2);
        assertThat(assignment.getRecipients())
                .extracting(recipient -> recipient.getStudentUser().getId())
                .containsExactly(STUDENT_ID, SECOND_STUDENT_ID);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    @DisplayName("publish: future open time produces scheduled assignment")
    void publish_whenOpenAtIsFuture_shouldScheduleAssignment() {
        Assignment assignment = buildDraftAssignmentWithTargets(List.of(buildStudentTarget(student)));
        assignment.setOpenAt(Instant.now().plusSeconds(3600));
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentDetailResponse response = service.publish(ASSIGNMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.SCHEDULED);
        assertThat(response.getRecipients()).hasSize(1);
    }

    @Test
    @DisplayName("publish: target with no active recipients throws BadRequestException")
    void publish_whenTargetsProduceNoRecipients_shouldThrowBadRequest() {
        Assignment assignment = buildDraftAssignmentWithTargets(List.of(buildClassTarget()));
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(classEnrollmentRepository.findAllByClazz_IdAndStatus(CLASS_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.publish(ASSIGNMENT_ID))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("close: active assignment becomes closed")
    void close_whenActive_shouldCloseAssignment() {
        Assignment assignment = buildAssignment(AssignmentStatus.ACTIVE);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentDetailResponse response = service.close(ASSIGNMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.CLOSED);
        assertThat(assignment.getUpdatedBy()).isEqualTo(teacher);
    }

    @Test
    @DisplayName("archive: only closed assignments can be archived")
    void archive_whenAssignmentIsClosed_shouldArchiveAssignment() {
        Assignment assignment = buildAssignment(AssignmentStatus.CLOSED);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentDetailResponse response = service.archive(ASSIGNMENT_ID);

        assertThat(response.getStatus()).isEqualTo(AssignmentStatus.ARCHIVED);
    }

    @Test
    @DisplayName("delete: only draft assignment can be soft deleted")
    void delete_whenDraft_shouldSoftDeleteAssignment() {
        Assignment assignment = buildAssignment(AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(ASSIGNMENT_ID, CENTER_ID))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(ASSIGNMENT_ID);

        assertThat(assignment.getDeletedAt()).isNotNull();
        assertThat(assignment.getUpdatedBy()).isEqualTo(teacher);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    @DisplayName("findAllForTeacher: returns paged assignment list")
    void findAllForTeacher_shouldReturnPagedList() {
        Assignment assignment = buildAssignment(AssignmentStatus.DRAFT);
        PageRequest pageable = PageRequest.of(0, 20);
        when(assignmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assignment), pageable, 1));

        Page<AssignmentListResponse> response = service.findAllForTeacher(
                "homework",
                AssessmentType.HOMEWORK,
                AssignmentStatus.DRAFT,
                CLASS_ID,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("Assignment");
    }

    @Test
    @DisplayName("student: returns only assignments where current student is recipient")
    void findAllForStudent_shouldReturnRecipientAssignments() {
        when(authorizationService.getCurrentUser()).thenReturn(student);
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
        AssignmentRecipient recipient = recipient(buildAssignment(AssignmentStatus.ACTIVE), student);
        when(assignmentRecipientRepository
                .findAllByStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNullOrderByAssignedAtDesc(
                        STUDENT_ID,
                        CENTER_ID
                )).thenReturn(List.of(recipient));

        List<StudentAssignmentListResponse> response = service.findAllForStudent();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRecipientId()).isEqualTo(recipient.getId());
        assertThat(response.get(0).getTitle()).isEqualTo("Assignment");
    }

    @Test
    @DisplayName("student: detail requires recipient ownership")
    void findByIdForStudent_whenRecipientMissing_shouldThrowResourceNotFound() {
        when(authorizationService.getCurrentUser()).thenReturn(student);
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
        when(assignmentRecipientRepository
                .findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                        ASSIGNMENT_ID,
                        STUDENT_ID,
                        CENTER_ID
                )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForStudent(ASSIGNMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("student: detail returns assignment snapshot")
    void findByIdForStudent_whenRecipientExists_shouldReturnSnapshotDetail() {
        when(authorizationService.getCurrentUser()).thenReturn(student);
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);
        Assignment assignment = buildAssignment(AssignmentStatus.ACTIVE);
        assignment.getItems().add(buildAssignmentItem(assignment));
        AssignmentRecipient recipient = recipient(assignment, student);
        when(assignmentRecipientRepository
                .findByAssignment_IdAndStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNull(
                        ASSIGNMENT_ID,
                        STUDENT_ID,
                        CENTER_ID
                )).thenReturn(Optional.of(recipient));

        StudentAssignmentDetailResponse response = service.findByIdForStudent(ASSIGNMENT_ID);

        assertThat(response.getRecipientId()).isEqualTo(recipient.getId());
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getOptions()).hasSize(2);
    }

    @Test
    @DisplayName("create: non teacher throws AccessDeniedException")
    void create_whenUserIsNotTeacher_shouldThrowAccessDenied() {
        User owner = user(99L, Role.OWNER, "Owner");
        when(authorizationService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.create(validClassAssignmentRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("create: missing tenant context throws BadRequestException")
    void create_whenTenantContextMissing_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(validClassAssignmentRequest()))
                .isInstanceOf(BadRequestException.class);
    }

    private AssignmentRequest validClassAssignmentRequest() {
        return AssignmentRequest.builder()
                .assessmentId(ASSESSMENT_ID)
                .title("Homework")
                .description("Do this homework")
                .attemptLimit(2)
                .targets(List.of(classTarget(CLASS_ID)))
                .build();
    }

    private AssignmentRequest validStudentAssignmentRequest() {
        return AssignmentRequest.builder()
                .assessmentId(ASSESSMENT_ID)
                .title("Student Homework")
                .targets(List.of(studentTarget(STUDENT_ID)))
                .build();
    }

    private AssignmentTargetRequest classTarget(Long classId) {
        return AssignmentTargetRequest.builder()
                .targetType(AssignmentTargetType.CLASS)
                .classId(classId)
                .build();
    }

    private AssignmentTargetRequest studentTarget(Long studentUserId) {
        return AssignmentTargetRequest.builder()
                .targetType(AssignmentTargetType.STUDENT)
                .studentUserId(studentUserId)
                .build();
    }

    private Assignment buildAssignment(AssignmentStatus status) {
        return Assignment.builder()
                .id(ASSIGNMENT_ID)
                .center(center)
                .assessment(buildAssessment(AssessmentStatus.PUBLISHED))
                .type(AssessmentType.HOMEWORK)
                .status(status)
                .title("Assignment")
                .description("Description")
                .createdBy(teacher)
                .updatedBy(teacher)
                .targets(new ArrayList<>())
                .recipients(new ArrayList<>())
                .items(new ArrayList<>())
                .build();
    }

    private Assignment buildDraftAssignmentWithTargets(List<AssignmentTarget> targets) {
        Assignment assignment = buildAssignment(AssignmentStatus.DRAFT);
        targets.forEach(target -> {
            target.setAssignment(assignment);
            assignment.getTargets().add(target);
        });
        return assignment;
    }

    private AssignmentTarget buildClassTarget() {
        return AssignmentTarget.builder()
                .targetType(AssignmentTargetType.CLASS)
                .clazz(buildClass(ClassStatus.ACTIVE))
                .build();
    }

    private AssignmentTarget buildStudentTarget(User targetStudent) {
        return AssignmentTarget.builder()
                .targetType(AssignmentTargetType.STUDENT)
                .studentUser(targetStudent)
                .build();
    }

    private AssignmentRecipient recipient(Assignment assignment, User targetStudent) {
        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .id(500L)
                .assignment(assignment)
                .studentUser(targetStudent)
                .sourceType(AssignmentTargetType.STUDENT)
                .status(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build();
        assignment.getRecipients().add(recipient);
        return recipient;
    }

    private com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem buildAssignmentItem(Assignment assignment) {
        com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem item =
                com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem.builder()
                .id(700L)
                .assignment(assignment)
                .assessmentItem(buildAssessmentItem())
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Question")
                .content("Content")
                .difficulty(QuestionDifficulty.EASY)
                .points(BigDecimal.ONE)
                .displayOrder(1)
                .options(new ArrayList<>())
                .build();
        item.getOptions().add(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption.builder()
                .id(701L)
                .assignmentItem(item)
                .content("A")
                .isCorrect(true)
                .displayOrder(1)
                .build());
        item.getOptions().add(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption.builder()
                .id(702L)
                .assignmentItem(item)
                .content("B")
                .isCorrect(false)
                .displayOrder(2)
                .build());
        return item;
    }

    private Assessment buildAssessment(AssessmentStatus status) {
        Assessment assessment = Assessment.builder()
                .id(ASSESSMENT_ID)
                .center(center)
                .type(AssessmentType.HOMEWORK)
                .status(status)
                .title("Assessment")
                .description("Assessment description")
                .createdBy(teacher)
                .updatedBy(teacher)
                .items(new ArrayList<>())
                .build();
        AssessmentItem item = buildAssessmentItem();
        item.setAssessment(assessment);
        assessment.getItems().add(item);
        return assessment;
    }

    private AssessmentItem buildAssessmentItem() {
        AssessmentItem item = AssessmentItem.builder()
                .id(600L)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Question")
                .content("Question content")
                .difficulty(QuestionDifficulty.EASY)
                .points(new BigDecimal("3.50"))
                .displayOrder(1)
                .options(new ArrayList<>())
                .build();
        item.getOptions().add(AssessmentItemOption.builder()
                .id(601L)
                .assessmentItem(item)
                .content("Correct")
                .isCorrect(true)
                .displayOrder(1)
                .build());
        item.getOptions().add(AssessmentItemOption.builder()
                .id(602L)
                .assessmentItem(item)
                .content("Wrong")
                .isCorrect(false)
                .displayOrder(2)
                .build());
        return item;
    }

    private ClassEnrollment enrollment(User targetStudent) {
        return ClassEnrollment.builder()
                .id(targetStudent.getId() + 1000L)
                .center(center)
                .clazz(buildClass(ClassStatus.ACTIVE))
                .studentUser(targetStudent)
                .status(EnrollmentStatus.ACTIVE)
                .build();
    }

    private com.owlexa.owlexabackend.modules.class_management.entity.Class buildClass(ClassStatus status) {
        return com.owlexa.owlexabackend.modules.class_management.entity.Class.builder()
                .id(CLASS_ID)
                .center(center)
                .name("Class A")
                .status(status)
                .build();
    }

    private User user(Long id, Role role, String fullName) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(fullName);
        return user;
    }
}
