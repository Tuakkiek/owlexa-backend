package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.homework.dto.request.*;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkPublishedEvent;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherHomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final HomeworkValidationService validationService;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveHomeworkTree(Long teacherId, Long homeworkId, TeacherHomeworkSaveRequest request) {
        Long centerId = TenantContext.getCurrentTenantId();
        
        validationService.validateTeacherAssignedToClass(request.getClazzId(), teacherId, centerId);

        Homework homework;
        if (homeworkId == null) {
            homework = new Homework();
            homework.setCenter(classRepository.findById(request.getClazzId()).orElseThrow().getCenter());
            homework.setTeacher(userRepository.findById(teacherId).orElseThrow());
            homework.setClazz(classRepository.findById(request.getClazzId()).orElseThrow());
            homework.setStatus(HomeworkStatus.DRAFT);
        } else {
            homework = homeworkRepository.findWithDetailsByIdAndCenter_IdAndTeacher_Id(homeworkId, centerId, teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Homework not found or access denied."));
            if (homework.getStatus() != HomeworkStatus.DRAFT) {
                throw new BusinessRuleException("Can only update DRAFT homework.");
            }
            homework.getQuestions().clear(); // Orphan removal handles the rest
        }

        homework.setTitle(request.getTitle());
        homework.setDescription(request.getDescription());
        homework.setInstructions(request.getInstructions());
        homework.setDueDate(request.getDueDate());
        homework.setAllowLateSubmission(request.getAllowLateSubmission() != null ? request.getAllowLateSubmission() : false);
        homework.setAllowResubmit(request.getAllowResubmit() != null ? request.getAllowResubmit() : false);
        homework.setPublishScoreImmediately(request.getPublishScoreImmediately() != null ? request.getPublishScoreImmediately() : false);
        homework.setShowAnswerAfterGrading(request.getShowAnswerAfterGrading() != null ? request.getShowAnswerAfterGrading() : false);
        homework.setMaxScore(request.getMaxScore());
        
        double totalHomeworkScore = 0.0;

        if (request.getQuestions() != null) {
            for (TeacherHomeworkQuestionRequest qRequest : request.getQuestions()) {
                HomeworkQuestion question = new HomeworkQuestion();
                question.setHomework(homework);
                question.setType(qRequest.getType());
                question.setQuestionText(qRequest.getQuestionText());
                question.setAttachedImageUrl(qRequest.getAttachedImageUrl());
                question.setAttachedAudioUrl(qRequest.getAttachedAudioUrl());
                question.setAttachedFileUrl(qRequest.getAttachedFileUrl());
                question.setSortOrder(qRequest.getSortOrder());
                
                double questionMaxScore = 0.0;

                if (qRequest.getType() == HomeworkQuestionType.QUIZ && qRequest.getOptions() != null) {
                    for (TeacherHomeworkQuestionOptionRequest oRequest : qRequest.getOptions()) {
                        HomeworkQuestionOption option = new HomeworkQuestionOption();
                        option.setQuestion(question);
                        option.setContent(oRequest.getContent());
                        option.setSortOrder(oRequest.getSortOrder());
                        option.setIsCorrect(oRequest.getIsCorrect());
                        question.getOptions().add(option);
                    }
                    // For quiz, the max score comes from the DTO, not calculated from options usually, or maybe it does? 
                    // Epic requirements implied maxScore is on Question. We'll sum it up. Wait, quiz options don't have maxScore.
                    // So we must use a field maxScore in QuestionRequest for Quiz. 
                    // Let's assume the user passes maxScore in qRequest? The DTO I wrote didn't have maxScore on Question Request!
                    // Wait! I need to add maxScore to TeacherHomeworkQuestionRequest. I will fix it.
                }

                if (qRequest.getType() == HomeworkQuestionType.ESSAY && qRequest.getRubric() != null) {
                    TeacherHomeworkRubricRequest rRequest = qRequest.getRubric();
                    HomeworkRubric rubric = new HomeworkRubric();
                    rubric.setQuestion(question);
                    rubric.setTitle(rRequest.getTitle());
                    rubric.setDescription(rRequest.getDescription());
                    
                    double rubricScore = 0.0;
                    if (rRequest.getCriteria() != null) {
                        for (TeacherHomeworkRubricCriterionRequest cRequest : rRequest.getCriteria()) {
                            HomeworkRubricCriterion criterion = new HomeworkRubricCriterion();
                            criterion.setRubric(rubric);
                            criterion.setName(cRequest.getName());
                            criterion.setDescription(cRequest.getDescription());
                            criterion.setMaxScore(cRequest.getMaxScore());
                            criterion.setDisplayOrder(cRequest.getDisplayOrder());
                            rubricScore += criterion.getMaxScore();
                            rubric.getCriteria().add(criterion);
                        }
                    }
                    rubric.setMaxScore(rubricScore);
                    question.setRubric(rubric);
                    questionMaxScore = rubricScore;
                }
                
                question.setMaxScore(qRequest.getMaxScore()); 
                homework.getQuestions().add(question);
            }
        }
        
        homeworkRepository.save(homework);
    }

    @Transactional
    public void deleteHomework(Long homeworkId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        Homework homework = homeworkRepository.findByIdAndCenter_Id(homeworkId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));

        if (!homework.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }

        if (homework.getStatus() != HomeworkStatus.DRAFT) {
            throw new BusinessRuleException("Can only delete DRAFT homework.");
        }

        homeworkRepository.delete(homework);
        
        eventPublisher.publishEvent(new HomeworkDeletedEvent(homeworkId, homework.getClazz().getId(), centerId));
    }
    
    @Transactional
    public void releaseGrades(Long homeworkId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        Homework homework = homeworkRepository.findByIdAndCenter_Id(homeworkId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));

        if (!homework.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }

        homework.setIsGradesReleased(true);
        homeworkRepository.save(homework);
    }

    @Transactional
    public void publishHomework(Long teacherId, Long homeworkId) {
        Long centerId = TenantContext.getCurrentTenantId();
        Homework homework = homeworkRepository.findWithDetailsByIdAndCenter_IdAndTeacher_Id(homeworkId, centerId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));
        
        if (homework.getStatus() != HomeworkStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT homework can be published.");
        }
        
        validationService.validateForPublish(homework);
        
        homework.setStatus(HomeworkStatus.PUBLISHED);
        homework.setPublishedAt(Instant.now());
        homeworkRepository.save(homework);
        
        int classSize = (int) classEnrollmentRepository.countByClazz_IdAndStatus(homework.getClazz().getId(), com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE);
        
        eventPublisher.publishEvent(new HomeworkPublishedEvent(homeworkId, homework.getClazz().getId(), centerId, classSize));
    }

    @Transactional
    public void closeHomework(Long teacherId, Long homeworkId) {
        Long centerId = TenantContext.getCurrentTenantId();
        Homework homework = homeworkRepository.findByIdAndCenter_IdAndTeacher_Id(homeworkId, centerId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));
        
        if (homework.getStatus() != HomeworkStatus.PUBLISHED) {
            throw new BusinessRuleException("Only PUBLISHED homework can be closed.");
        }
        
        homework.setStatus(HomeworkStatus.CLOSED);
        homework.setClosedAt(Instant.now());
        homeworkRepository.save(homework);
    }
}
