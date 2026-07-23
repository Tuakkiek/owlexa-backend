package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.*;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHomeworkService {

    private final HomeworkRepository homeworkRepository;
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

        List<Homework> homeworks = homeworkRepository.findAllByClazz_IdInAndStatusInAndCenter_Id(
                classIds,
                List.of(HomeworkStatus.PUBLISHED, HomeworkStatus.CLOSED),
                centerId
        );

        return homeworks.stream().map(hw -> {
            StudentHomeworkListResponse response = new StudentHomeworkListResponse();
            response.setId(hw.getId());
            response.setTitle(hw.getTitle());
            response.setStatus(hw.getStatus());
            response.setDueDate(hw.getDueDate());
            response.setMaxScore(hw.getMaxScore());
            response.setAllowLateSubmission(hw.getAllowLateSubmission());
            response.setClazzId(hw.getClazz().getId());
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentHomeworkDetailResponse getHomeworkDetails(Long studentId, Long homeworkId) {
        Long centerId = TenantContext.getCurrentTenantId();

        Homework homework = homeworkRepository.findWithDetailsByIdAndCenter_Id(homeworkId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));
                
        if (homework.getStatus() == HomeworkStatus.DRAFT) {
            throw new ResourceNotFoundException("Homework not found.");
        }

        boolean isEnrolled = classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                homework.getClazz().getId(), studentId, EnrollmentStatus.ACTIVE);
        
        if (!isEnrolled) {
            throw new ResourceNotFoundException("Homework not found or access denied.");
        }

        return mapToDetailResponse(homework);
    }
    
    private StudentHomeworkDetailResponse mapToDetailResponse(Homework homework) {
        StudentHomeworkDetailResponse response = new StudentHomeworkDetailResponse();
        response.setId(homework.getId());
        response.setTitle(homework.getTitle());
        response.setDescription(homework.getDescription());
        response.setInstructions(homework.getInstructions());
        response.setStatus(homework.getStatus());
        response.setDueDate(homework.getDueDate());
        response.setPublishedAt(homework.getPublishedAt());
        response.setClosedAt(homework.getClosedAt());
        response.setMaxScore(homework.getMaxScore());
        response.setAllowLateSubmission(homework.getAllowLateSubmission());
        response.setAllowResubmit(homework.getAllowResubmit());
        response.setPublishScoreImmediately(homework.getPublishScoreImmediately());
        response.setShowAnswerAfterGrading(homework.getShowAnswerAfterGrading());
        response.setClazzId(homework.getClazz().getId());
        
        if (homework.getQuestions() != null) {
            List<StudentHomeworkQuestionResponse> questionResponses = homework.getQuestions().stream().map(q -> {
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
