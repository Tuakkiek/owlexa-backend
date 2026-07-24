package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.*;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHomeworkService {

    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    @Transactional(readOnly = true)
    public List<StudentHomeworkListResponse> getMyHomeworks(Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                studentId, centerId, List.of(EnrollmentStatus.ACTIVE)
        );

        List<Long> classIds = enrollments.stream()
                .map(e -> e.getClazz().getId())
                .collect(Collectors.toList());

        if (classIds.isEmpty()) {
            return List.of();
        }

        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findAllByClazz_IdInAndStatusInAndCenter_Id(
                classIds,
                List.of(HomeworkAssignmentStatus.SCHEDULED, HomeworkAssignmentStatus.OPEN, HomeworkAssignmentStatus.CLOSED),
                centerId
        );

        return assignments.stream().map(hw -> {
            StudentHomeworkListResponse response = new StudentHomeworkListResponse();
            response.setId(hw.getId());
            response.setTitle(hw.getHomeworkTemplate().getTitle());
            response.setStatus(hw.getStatus());
            response.setAvailableFrom(hw.getAvailableFrom());
            response.setDueDate(hw.getDueDate());
            response.setCloseAt(hw.getCloseAt());
            response.setMaxScore(hw.getHomeworkTemplate().getMaxScore());
            response.setAllowLateSubmission(hw.getAllowLateSubmission());
            response.setClazzId(hw.getClazz().getId());
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentHomeworkDetailResponse getHomeworkDetails(Long studentId, Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkAssignment assignment = homeworkAssignmentRepository.findWithTemplateByIdAndCenter_Id(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));
                
        if (assignment.getStatus() == HomeworkAssignmentStatus.DRAFT) {
            throw new ResourceNotFoundException("Homework not found.");
        }

        boolean isEnrolled = classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                assignment.getClazz().getId(), studentId, EnrollmentStatus.ACTIVE);
        
        if (!isEnrolled) {
            throw new ResourceNotFoundException("Homework not found or access denied.");
        }

        return mapToDetailResponse(assignment);
    }
    
    private StudentHomeworkDetailResponse mapToDetailResponse(HomeworkAssignment assignment) {
        StudentHomeworkDetailResponse response = new StudentHomeworkDetailResponse();
        response.setId(assignment.getId());
        response.setTitle(assignment.getHomeworkTemplate().getTitle());
        response.setDescription(assignment.getHomeworkTemplate().getDescription());
        response.setInstructions(assignment.getHomeworkTemplate().getInstructions());
        response.setStatus(assignment.getStatus());
        response.setHomeworkType(assignment.getHomeworkTemplate().getHomeworkType());
        response.setEstimatedTime(assignment.getHomeworkTemplate().getEstimatedTime());

        response.setAvailableFrom(assignment.getAvailableFrom());
        response.setDueDate(assignment.getDueDate());
        response.setCloseAt(assignment.getCloseAt());
        response.setMaxScore(assignment.getHomeworkTemplate().getMaxScore());
        response.setAllowLateSubmission(assignment.getAllowLateSubmission());
        response.setAllowResubmit(assignment.getAllowResubmit());
        response.setPublishScoreImmediately(assignment.getPublishScoreImmediately());
        response.setShowAnswerAfterGrading(assignment.getShowAnswerAfterGrading());
        response.setClazzId(assignment.getClazz().getId());
        
        if (assignment.getHomeworkTemplate().getQuestions() != null) {
            List<StudentHomeworkQuestionResponse> questionResponses = assignment.getHomeworkTemplate().getQuestions().stream().map(q -> {
                StudentHomeworkQuestionResponse qRes = new StudentHomeworkQuestionResponse();
                qRes.setId(q.getId());
                qRes.setType(q.getType());
                qRes.setQuestionText(q.getQuestionText());
                qRes.setAttachedImageUrl(q.getAttachedImageUrl());
                qRes.setAttachedAudioUrl(q.getAttachedAudioUrl());
                qRes.setAttachedFileUrl(q.getAttachedFileUrl());
                qRes.setSortOrder(q.getSortOrder());
                qRes.setMaxScore(q.getMaxScore());
                
                if (q.getOptions() != null) {
                    qRes.setOptions(q.getOptions().stream().map(o -> {
                        StudentHomeworkOptionResponse oRes = new StudentHomeworkOptionResponse();
                        oRes.setId(o.getId());
                        oRes.setContent(o.getContent());
                        oRes.setSortOrder(o.getSortOrder());
                        return oRes;
                    }).collect(Collectors.toList()));
                }
                
                if (q.getRubric() != null) {
                    StudentHomeworkRubricResponse rRes = new StudentHomeworkRubricResponse();
                    rRes.setId(q.getRubric().getId());
                    rRes.setTitle(q.getRubric().getTitle());
                    rRes.setDescription(q.getRubric().getDescription());
                    rRes.setMaxScore(q.getRubric().getMaxScore());
                    
                    if (q.getRubric().getCriteria() != null) {
                        rRes.setCriteria(q.getRubric().getCriteria().stream().map(c -> {
                            StudentHomeworkRubricCriterionResponse cRes = new StudentHomeworkRubricCriterionResponse();
                            cRes.setId(c.getId());
                            cRes.setName(c.getName());
                            cRes.setDescription(c.getDescription());
                            cRes.setMaxScore(c.getMaxScore());
                            cRes.setDisplayOrder(c.getDisplayOrder());
                            return cRes;
                        }).collect(Collectors.toList()));
                    }
                    qRes.setRubric(rRes);
                }
                
                return qRes;
            }).collect(Collectors.toList());
            
            response.setQuestions(questionResponses);
        }
        
        return response;
    }
}
