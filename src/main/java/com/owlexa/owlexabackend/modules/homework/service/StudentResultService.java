package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.*;
import com.owlexa.owlexabackend.modules.homework.entity.Homework;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionOption;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionSubmission;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentResultService {

    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkRepository homeworkRepository;

    @Transactional(readOnly = true)
    public List<StudentResultSummaryResponse> getAttemptResults(Long homeworkId, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        
        // Validate access and existence
        homeworkRepository.findByIdAndCenter_Id(homeworkId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));
                
        List<HomeworkSubmission> submissions = submissionRepository.findAllByHomework_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(homeworkId, centerId, studentId);
        
        return submissions.stream().map(this::mapToSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResultDetailResponse getResultDetails(Long submissionId, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findWithDetailsByIdAndCenter_IdAndStudent_Id(submissionId, centerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        Homework homework = submission.getHomework();
        
        // Visibility Check
        boolean isResultVisible = false;
        if (submission.getStatus() == HomeworkSubmissionStatus.GRADED || submission.getStatus() == HomeworkSubmissionStatus.RETURNED) {
            if (Boolean.TRUE.equals(homework.getPublishScoreImmediately())) {
                isResultVisible = true;
            } else if (Boolean.TRUE.equals(homework.getIsGradesReleased())) {
                isResultVisible = true;
            }
        }
        
        if (!isResultVisible) {
            throw new BusinessRuleException("Grades are not yet released for this submission.");
        }

        return mapToDetailResponse(submission, homework.getShowAnswerAfterGrading());
    }

    private StudentResultSummaryResponse mapToSummary(HomeworkSubmission s) {
        StudentResultSummaryResponse r = new StudentResultSummaryResponse();
        r.setId(s.getId());
        r.setHomeworkId(s.getHomework().getId());
        r.setAttemptNumber(s.getAttemptNumber());
        r.setStatus(s.getStatus());
        r.setSubmittedAt(s.getSubmittedAt());
        r.setGradedAt(s.getGradedAt());
        
        double total = 0.0;
        if (s.getQuestionSubmissions() != null) {
             for (var qs : s.getQuestionSubmissions()) {
                 if (qs.getTeacherOverrideScore() != null) total += qs.getTeacherOverrideScore();
                 else if (qs.getScore() != null) total += qs.getScore();
             }
        }
        r.setTotalScore(total);
        return r;
    }

    private StudentResultDetailResponse mapToDetailResponse(HomeworkSubmission s, boolean showAnswers) {
        StudentResultDetailResponse r = new StudentResultDetailResponse();
        r.setId(s.getId());
        r.setHomeworkId(s.getHomework().getId());
        r.setAttemptNumber(s.getAttemptNumber());
        r.setStatus(s.getStatus());
        r.setSubmittedAt(s.getSubmittedAt());
        r.setGradedAt(s.getGradedAt());
        r.setTeacherFeedback(s.getTeacherFeedback());

        double total = 0.0;
        List<StudentResultQuestionResponse> qResponses = new ArrayList<>();
        
        for (HomeworkQuestionSubmission qs : s.getQuestionSubmissions()) {
            StudentResultQuestionResponse qRes = new StudentResultQuestionResponse();
            qRes.setId(qs.getId());
            qRes.setQuestionId(qs.getQuestion().getId());
            qRes.setTextAnswer(qs.getTextAnswer());
            
            Double effectiveScore = qs.getTeacherOverrideScore() != null ? qs.getTeacherOverrideScore() : (qs.getScore() != null ? qs.getScore() : 0.0);
            qRes.setEffectiveScore(effectiveScore);
            total += effectiveScore;
            
            qRes.setTeacherFeedback(qs.getTeacherFeedback());
            qRes.setAiFeedback(qs.getAiFeedback());

            if (qs.getAttachments() != null) {
                qRes.setAttachments(qs.getAttachments().stream().map(a -> {
                    StudentHomeworkSubmissionAttachmentResponse att = new StudentHomeworkSubmissionAttachmentResponse();
                    att.setId(a.getId());
                    att.setFileName(a.getFileName());
                    att.setFileUrl(a.getFileUrl());
                    att.setFileType(a.getFileType());
                    return att;
                }).collect(Collectors.toList()));
            }

            if (qs.getSelectedOptions() != null) {
                qRes.setSelectedOptionIds(qs.getSelectedOptions().stream().map(o -> o.getOption().getId()).collect(Collectors.toList()));
            }

            if (qs.getCriterionScores() != null) {
                qRes.setCriterionScores(qs.getCriterionScores().stream().map(cs -> {
                    StudentResultRubricScoreResponse csRes = new StudentResultRubricScoreResponse();
                    csRes.setId(cs.getId());
                    csRes.setCriterionId(cs.getCriterion().getId());
                    csRes.setScore(cs.getScore());
                    csRes.setComment(cs.getComment());
                    csRes.setGraderType(cs.getGraderType());
                    return csRes;
                }).collect(Collectors.toList()));
            }
            
            // Visibility Rules for cheating prevention
            if (showAnswers && qs.getQuestion().getType() == HomeworkQuestionType.QUIZ) {
                qRes.setIsCorrect(qs.getIsCorrect());
                
                // Expose correct options
                List<Long> correctIds = qs.getQuestion().getOptions().stream()
                        .filter(HomeworkQuestionOption::getIsCorrect)
                        .map(HomeworkQuestionOption::getId)
                        .collect(Collectors.toList());
                qRes.setCorrectOptionIds(correctIds);
            }
            
            qResponses.add(qRes);
        }
        
        r.setQuestions(qResponses);
        r.setTotalScore(total);
        return r;
    }
}
