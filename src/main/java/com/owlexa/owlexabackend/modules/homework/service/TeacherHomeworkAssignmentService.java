package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkDeletedEvent;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkPublishedEvent;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherHomeworkAssignmentSaveRequest;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkTemplate;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkTemplateRepository;
import lombok.RequiredArgsConstructor;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TeacherHomeworkAssignmentService {

    private final HomeworkAssignmentRepository assignmentRepository;
    private final HomeworkTemplateRepository templateRepository;
    private final ClassRepository classRepository;
    private final HomeworkValidationService validationService;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final HomeworkAssignmentStateService stateService;
    private final com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository submissionRepository;
    private final Clock clock;

    @Transactional
    public void assignHomework(Long teacherId, TeacherHomeworkAssignmentSaveRequest request) {
        Long centerId = TenantContext.getCurrentTenantId();

        validationService.validateTeacherAssignedToClass(request.getClazzId(), teacherId, centerId);

        HomeworkTemplate template = templateRepository.findByIdAndCenter_Id(request.getTemplateId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found."));

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setHomeworkTemplate(template);
        assignment.setClazz(classRepository.findById(request.getClazzId()).orElseThrow());
        assignment.setTeacher(userRepository.findById(teacherId).orElseThrow());
        assignment.setCenter(template.getCenter());
        
        if (request.getAvailableFrom() != null && request.getAvailableFrom().isAfter(Instant.now(clock))) {
            assignment.setStatus(HomeworkAssignmentStatus.SCHEDULED);
        } else {
            assignment.setStatus(HomeworkAssignmentStatus.OPEN);
        }
        
        assignment.setAvailableFrom(request.getAvailableFrom());
        assignment.setDueDate(request.getDueDate());
        assignment.setCloseAt(request.getCloseAt());
        
        stateService.validateTimeline(request.getAvailableFrom(), request.getDueDate(), request.getCloseAt(), clock);
        
        assignment.setAllowLateSubmission(request.getAllowLateSubmission() != null ? request.getAllowLateSubmission() : false);
        assignment.setAllowResubmit(request.getAllowResubmit() != null ? request.getAllowResubmit() : false);
        assignment.setPublishScoreImmediately(request.getPublishScoreImmediately() != null ? request.getPublishScoreImmediately() : false);
        assignment.setShowAnswerAfterGrading(request.getShowAnswerAfterGrading() != null ? request.getShowAnswerAfterGrading() : false);
        
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void updateAssignment(Long teacherId, Long assignmentId, TeacherHomeworkAssignmentSaveRequest request) {
        Long centerId = TenantContext.getCurrentTenantId();
        
        HomeworkAssignment assignment = assignmentRepository.findByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
                
        boolean hasSubmissions = submissionRepository.existsByHomeworkAssignment_Id(assignmentId);
                
        if (hasSubmissions || assignment.getStatus() == HomeworkAssignmentStatus.OPEN || assignment.getStatus() == HomeworkAssignmentStatus.CLOSED) {
            // Limited updates
            if (request.getAvailableFrom() != null) {
                assignment.setAvailableFrom(request.getAvailableFrom());
            }
            if (request.getDueDate() != null) {
                assignment.setDueDate(request.getDueDate()); // Allow extending due date
            }
            if (request.getCloseAt() != null) {
                assignment.setCloseAt(request.getCloseAt());
            }
            if (request.getPublishScoreImmediately() != null) {
                assignment.setPublishScoreImmediately(request.getPublishScoreImmediately());
            }
        } else if (assignment.getStatus() == HomeworkAssignmentStatus.DRAFT || assignment.getStatus() == HomeworkAssignmentStatus.SCHEDULED) {
            validationService.validateTeacherAssignedToClass(request.getClazzId(), teacherId, centerId);
            stateService.validateTimeline(request.getAvailableFrom(), request.getDueDate(), request.getCloseAt(), clock);
            
            assignment.setClazz(classRepository.findById(request.getClazzId()).orElseThrow());
            assignment.setAvailableFrom(request.getAvailableFrom());
            assignment.setDueDate(request.getDueDate());
            assignment.setCloseAt(request.getCloseAt());
            assignment.setAllowLateSubmission(request.getAllowLateSubmission() != null ? request.getAllowLateSubmission() : false);
            assignment.setAllowResubmit(request.getAllowResubmit() != null ? request.getAllowResubmit() : false);
            assignment.setPublishScoreImmediately(request.getPublishScoreImmediately() != null ? request.getPublishScoreImmediately() : false);
            assignment.setShowAnswerAfterGrading(request.getShowAnswerAfterGrading() != null ? request.getShowAnswerAfterGrading() : false);
        } else {
            throw new BusinessRuleException("Cannot update assignment in current status.");
        }
        
        if (request.getStatus() != null && request.getStatus() != assignment.getStatus()) {
            stateService.transitionTo(assignment, request.getStatus(), clock);
        }
        
        assignmentRepository.save(assignment);
    }
    
    @Transactional
    public void deleteAssignment(Long assignmentId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkAssignment assignment = assignmentRepository.findByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        if (!assignment.getHomeworkTemplate().getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }

        stateService.transitionTo(assignment, HomeworkAssignmentStatus.ARCHIVED, clock);
        assignmentRepository.save(assignment);
        
        // Use existing event if it takes assignment ID instead of homework ID now
        eventPublisher.publishEvent(new HomeworkDeletedEvent(assignmentId, assignment.getClazz().getId(), centerId));
    }

    @Transactional
    public void releaseGrades(Long assignmentId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkAssignment assignment = assignmentRepository.findByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));

        if (!assignment.getHomeworkTemplate().getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }

        assignment.setIsGradesReleased(true);
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void scheduleAssignment(Long teacherId, Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkAssignment assignment = assignmentRepository.findWithTemplateByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        
        if (assignment.getStatus() != HomeworkAssignmentStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT assignment can be scheduled.");
        }
        
        validationService.validateForPublish(assignment);
        
        stateService.transitionTo(assignment, HomeworkAssignmentStatus.SCHEDULED, clock);
        assignmentRepository.save(assignment);
        
        int classSize = (int) classEnrollmentRepository.countByClazz_IdAndStatus(assignment.getClazz().getId(), com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE);
        
        eventPublisher.publishEvent(new HomeworkPublishedEvent(assignmentId, assignment.getClazz().getId(), centerId, classSize));
    }

    @Transactional
    public void closeAssignment(Long teacherId, Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkAssignment assignment = assignmentRepository.findWithTemplateByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        
        stateService.transitionTo(assignment, HomeworkAssignmentStatus.CLOSED, clock);
        assignmentRepository.save(assignment);
    }
    
    @Transactional
    public void cancelAssignment(Long teacherId, Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkAssignment assignment = assignmentRepository.findWithTemplateByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
                
        if (!assignment.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }
        
        stateService.transitionTo(assignment, HomeworkAssignmentStatus.CANCELLED, clock);
        assignmentRepository.save(assignment);
    }
    
    @Transactional
    public void reopenAssignment(Long teacherId, Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkAssignment assignment = assignmentRepository.findWithTemplateByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
                
        if (!assignment.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }
        
        if (assignment.getStatus() != HomeworkAssignmentStatus.CLOSED && assignment.getStatus() != HomeworkAssignmentStatus.CANCELLED) {
            throw new BusinessRuleException("Can only reopen CLOSED or CANCELLED assignments.");
        }
        
        stateService.transitionTo(assignment, HomeworkAssignmentStatus.OPEN, clock);
        assignment.setCloseAt(null); // Clear close time if reopened, teacher should set it again
        assignmentRepository.save(assignment);
    }
    
    @Transactional(readOnly = true)
    public java.util.List<HomeworkAssignment> getDashboardAssignments(Long teacherId, String category) {
        Long centerId = TenantContext.getCurrentTenantId();
        // Placeholder for dashboard advanced logic.
        java.util.List<HomeworkAssignment> all = assignmentRepository.findAllByCenter_Id(centerId).stream()
                .filter(a -> a.getTeacher().getId().equals(teacherId))
                .collect(java.util.stream.Collectors.toList());
                
        java.time.Instant now = java.time.Instant.now(clock);
        
        switch (category) {
            case "UPCOMING":
                return all.stream()
                        .filter(a -> a.getStatus() == HomeworkAssignmentStatus.SCHEDULED)
                        .collect(java.util.stream.Collectors.toList());
            case "OPEN":
                return all.stream()
                        .filter(a -> a.getStatus() == HomeworkAssignmentStatus.OPEN)
                        .collect(java.util.stream.Collectors.toList());
            case "CLOSING_SOON":
                return all.stream()
                        .filter(a -> a.getStatus() == HomeworkAssignmentStatus.OPEN && a.getCloseAt() != null && a.getCloseAt().minus(java.time.Duration.ofHours(24)).isBefore(now))
                        .collect(java.util.stream.Collectors.toList());
            case "RECENTLY_CLOSED":
                return all.stream()
                        .filter(a -> a.getStatus() == HomeworkAssignmentStatus.CLOSED && a.getClosedAt() != null && a.getClosedAt().plus(java.time.Duration.ofDays(7)).isAfter(now))
                        .collect(java.util.stream.Collectors.toList());
            case "DRAFTS":
                return all.stream()
                        .filter(a -> a.getStatus() == HomeworkAssignmentStatus.DRAFT)
                        .collect(java.util.stream.Collectors.toList());
            default:
                return all;
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkAssignmentResponse> searchAssignments(
            Long teacherId, String keyword, Long classId, HomeworkAssignmentStatus status, com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type) {
        Long centerId = TenantContext.getCurrentTenantId();
        
        return assignmentRepository.findAllByCenter_Id(centerId).stream()
                .filter(a -> a.getTeacher().getId().equals(teacherId))
                .filter(a -> classId == null || a.getClazz().getId().equals(classId))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> type == null || a.getHomeworkTemplate().getHomeworkType() == type)
                .filter(a -> keyword == null || keyword.isEmpty() || a.getHomeworkTemplate().getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkAssignmentResponse mapToResponse(HomeworkAssignment assignment) {
        long totalStudents = classEnrollmentRepository.countByClazz_IdAndStatus(assignment.getClazz().getId(), com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE);
        long submittedCount = submissionRepository.countByHomeworkAssignment_Id(assignment.getId());
        long gradedCount = submissionRepository.countByHomeworkAssignment_IdAndStatus(assignment.getId(), com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus.GRADED);
        
        long missingCount = Math.max(0, totalStudents - submittedCount);
        long lateCount = 0;
        if (assignment.getDueDate() != null) {
            lateCount = submissionRepository.countLateSubmissions(assignment.getId(), assignment.getDueDate());
        }

        return com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkAssignmentResponse.builder()
                .id(assignment.getId())
                .templateId(assignment.getHomeworkTemplate().getId())
                .templateTitle(assignment.getHomeworkTemplate().getTitle())
                .maxScore(assignment.getHomeworkTemplate().getMaxScore())
                .clazzId(assignment.getClazz().getId())
                .clazzName(assignment.getClazz().getName())
                .teacherId(assignment.getTeacher().getId())
                .teacherFullName(assignment.getTeacher().getFullName())
                .status(assignment.getStatus())
                .availableFrom(assignment.getAvailableFrom())
                .dueDate(assignment.getDueDate())
                .closeAt(assignment.getCloseAt())
                .scheduledAt(assignment.getScheduledAt())
                .openedAt(assignment.getOpenedAt())
                .closedAt(assignment.getClosedAt())
                .archivedAt(assignment.getArchivedAt())
                .cancelledAt(assignment.getCancelledAt())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .allowLateSubmission(assignment.getAllowLateSubmission())
                .allowResubmit(assignment.getAllowResubmit())
                .publishScoreImmediately(assignment.getPublishScoreImmediately())
                .isGradesReleased(assignment.getIsGradesReleased())
                .showAnswerAfterGrading(assignment.getShowAnswerAfterGrading())
                .totalStudents(totalStudents)
                .submittedCount(submittedCount)
                .gradedCount(gradedCount)
                .missingCount(missingCount)
                .lateCount(lateCount)
                .build();
    }
}
