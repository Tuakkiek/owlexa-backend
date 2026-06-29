package com.owlexa.owlexabackend.modules.essay.service;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssayRubricRequest;
import com.owlexa.owlexabackend.modules.essay.dto.request.EssaySubmitRequest;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayDetailResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayGradingResultResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssayRubricResponse;
import com.owlexa.owlexabackend.modules.essay.dto.response.EssaySubmissionResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.essay.entity.EssayCriteriaScore;
import com.owlexa.owlexabackend.modules.essay.entity.EssayGradingResult;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubric;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubricCriterion;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmission;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmissionStatus;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.filter.TenantFilter;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayGradingResultRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayRubricRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssaySubmissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EssayService {

    private final EssayRubricRepository essayRubricRepository;
    private final EssaySubmissionRepository essaySubmissionRepository;
    private final EssayGradingResultRepository essayGradingResultRepository;
    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<EssayRubricResponse> findMyRubricsAsTeacher() {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return essayRubricRepository
                .findAllByCreatedByUserIdAndCenterId(currentUser.getId(), centerId)
                .stream()
                .map(this::toRubricResponse)
                .toList();
    }

    @Transactional
    public EssayRubricResponse createRubric(EssayRubricRequest request) {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        if (request.getClassId() == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Class and rubric title are required");
        }

        Class clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getClassId()));
        if (!clazz.getCenter().getId().equals(centerId) || !teacherTeachesClass(currentUser.getId(), clazz.getId(), centerId)) {
            throw new AccessDeniedException("You do not have permission to create rubric for this class");
        }

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        EssayRubric rubric = EssayRubric.builder()
                .clazz(clazz)
                .center(center)
                .createdByUser(currentUser)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .maxScore(request.getMaxScore() == null ? 10.0 : request.getMaxScore())
                .isActive(true)
                .build();

        List<EssayRubricRequest.CriterionRequest> criteria = request.getCriteria() == null
                ? List.of()
                : request.getCriteria();
        criteria.forEach(item -> rubric.getCriteria().add(EssayRubricCriterion.builder()
                .rubric(rubric)
                .name(item.getName())
                .description(item.getDescription())
                .weight(item.getWeight() == null ? 0.0 : item.getWeight())
                .maxScore(item.getMaxScore() == null ? 0.0 : item.getMaxScore())
                .build()));

        return toRubricResponse(essayRubricRepository.save(rubric));
    }

    @Transactional
    public EssaySubmissionResponse submitEssay(EssaySubmitRequest request) {
        User currentUser = requireCurrentUser(Role.STUDENT);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        if (request.getRubricId() == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("Rubric and essay content are required");
        }

        EssayRubric rubric = essayRubricRepository.findById(request.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric not found with id: " + request.getRubricId()));
        if (!rubric.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to use this rubric");
        }
        if (!classEnrollmentRepository.existsByClazzIdAndStudentUserIdAndStatus(
                rubric.getClazz().getId(),
                currentUser.getId(),
                EnrollmentStatus.ACTIVE
        )) {
            throw new AccessDeniedException("You are not enrolled in this class");
        }

        EssaySubmission submission = EssaySubmission.builder()
                .studentUser(currentUser)
                .clazz(rubric.getClazz())
                .rubric(rubric)
                .center(rubric.getCenter())
                .content(request.getContent().trim())
                .status(EssaySubmissionStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();

        submission = essaySubmissionRepository.save(submission);
        gradeSubmission(submission);
        submission.setStatus(EssaySubmissionStatus.GRADED);
        return toSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<EssaySubmissionResponse> findMyEssays() {
        User currentUser = requireCurrentUser(Role.STUDENT);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return essaySubmissionRepository
                .findAllByStudentUserIdAndCenterIdOrderByCreatedAtDesc(currentUser.getId(), centerId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EssayDetailResponse getEssay(Long essayId) {
        EssaySubmission submission = findVisibleSubmission(essayId);
        return EssayDetailResponse.builder()
                .essay(toSubmissionResponse(submission))
                .gradingResult(essayGradingResultRepository.findBySubmissionId(essayId)
                        .map(this::toGradingResponse)
                        .orElse(null))
                .build();
    }

    @Transactional(readOnly = true)
    public EssayGradingResultResponse getGradingResult(Long essayId) {
        findVisibleSubmission(essayId);
        return essayGradingResultRepository.findBySubmissionId(essayId)
                .map(this::toGradingResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EssaySubmissionResponse> findClassEssays(Long classId) {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);
        if (!teacherTeachesClass(currentUser.getId(), classId, centerId)) {
            throw new AccessDeniedException("You do not have permission to view essays for this class");
        }

        return essaySubmissionRepository.findAllByClazzIdAndCenterIdOrderByCreatedAtDesc(classId, centerId)
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional
    public void addManualFeedback(Long essayId, String feedback) {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        EssaySubmission submission = essaySubmissionRepository.findById(essayId)
                .orElseThrow(() -> new ResourceNotFoundException("Essay not found with id: " + essayId));
        if (!submission.getCenter().getId().equals(centerId)
                || !teacherTeachesClass(currentUser.getId(), submission.getClazz().getId(), centerId)) {
            throw new AccessDeniedException("You do not have permission to review this essay");
        }

        EssayGradingResult result = essayGradingResultRepository.findBySubmissionId(essayId)
                .orElseThrow(() -> new ResourceNotFoundException("Grading result not found for essay: " + essayId));
        result.setFeedback(feedback == null ? "" : feedback.trim());
        submission.setStatus(EssaySubmissionStatus.REVIEWED);
    }

    private EssayGradingResult gradeSubmission(EssaySubmission submission) {
        int words = submission.getContent().split("\\s+").length;
        double qualityRatio = Math.max(0.35, Math.min(1.0, words / 220.0));

        EssayGradingResult result = EssayGradingResult.builder()
                .submission(submission)
                .totalScore(0.0)
                .maxScore(submission.getRubric().getMaxScore())
                .feedback("Bai viet da duoc cham dua tren rubric hien tai. Hay bo sung y, vi du va cau truc ro hon de cai thien diem.")
                .build();

        double total = 0.0;
        for (EssayRubricCriterion criterion : submission.getRubric().getCriteria()) {
            double score = roundOne(criterion.getMaxScore() * qualityRatio);
            total += score;
            result.getCriteriaScores().add(EssayCriteriaScore.builder()
                    .gradingResult(result)
                    .criterion(criterion)
                    .score(score)
                    .maxScore(criterion.getMaxScore())
                    .feedback("Diem duoc tinh theo do day bai viet va trong so tieu chi.")
                    .build());
        }

        if (submission.getRubric().getCriteria().isEmpty()) {
            total = roundOne(submission.getRubric().getMaxScore() * qualityRatio);
        }

        result.setTotalScore(Math.min(submission.getRubric().getMaxScore(), roundOne(total)));
        return essayGradingResultRepository.save(result);
    }

    private EssaySubmission findVisibleSubmission(Long essayId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        EssaySubmission submission = essaySubmissionRepository.findById(essayId)
                .orElseThrow(() -> new ResourceNotFoundException("Essay not found with id: " + essayId));
        if (!submission.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to view this essay");
        }
        if (currentUser.getRole() == Role.STUDENT && submission.getStudentUser().getId().equals(currentUser.getId())) {
            return submission;
        }
        if (currentUser.getRole() == Role.TEACHER
                && teacherTeachesClass(currentUser.getId(), submission.getClazz().getId(), centerId)) {
            return submission;
        }
        throw new AccessDeniedException("You do not have permission to view this essay");
    }

    private boolean teacherTeachesClass(Long teacherUserId, Long classId, Long centerId) {
        return scheduleRepository.findAllByTeacherUserIdAndCenterId(teacherUserId, centerId)
                .stream()
                .anyMatch(schedule -> schedule.getClazz().getId().equals(classId));
    }

    private EssayRubricResponse toRubricResponse(EssayRubric rubric) {
        return EssayRubricResponse.builder()
                .id(rubric.getId())
                .classId(rubric.getClazz().getId())
                .className(rubric.getClazz().getName())
                .title(rubric.getTitle())
                .description(rubric.getDescription())
                .maxScore(rubric.getMaxScore())
                .criteria(rubric.getCriteria().stream()
                        .map(item -> EssayRubricResponse.CriterionResponse.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .description(item.getDescription())
                                .weight(item.getWeight())
                                .maxScore(item.getMaxScore())
                                .build())
                        .toList())
                .createdAt(rubric.getCreatedAt())
                .isActive(rubric.getIsActive())
                .build();
    }

    private EssaySubmissionResponse toSubmissionResponse(EssaySubmission submission) {
        return EssaySubmissionResponse.builder()
                .id(submission.getId())
                .studentId(submission.getStudentUser().getId())
                .studentFullName(submission.getStudentUser().getFullName())
                .classId(submission.getClazz().getId())
                .className(submission.getClazz().getName())
                .rubricId(submission.getRubric().getId())
                .rubricTitle(submission.getRubric().getTitle())
                .content(submission.getContent())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private EssayGradingResultResponse toGradingResponse(EssayGradingResult result) {
        return EssayGradingResultResponse.builder()
                .id(result.getId())
                .submissionId(result.getSubmission().getId())
                .totalScore(result.getTotalScore())
                .maxScore(result.getMaxScore())
                .criteriaScores(result.getCriteriaScores().stream()
                        .map(score -> EssayGradingResultResponse.CriteriaScoreResponse.builder()
                                .criteriaId(score.getCriterion().getId())
                                .criteriaName(score.getCriterion().getName())
                                .score(score.getScore())
                                .maxScore(score.getMaxScore())
                                .feedback(score.getFeedback())
                                .build())
                        .toList())
                .feedback(result.getFeedback())
                .gradedAt(result.getGradedAt())
                .build();
    }

    private User requireCurrentUser(Role role) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != role) {
            throw new AccessDeniedException("Only " + role + " can access this resource");
        }
        return currentUser;
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        if (!membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
