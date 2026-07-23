package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.homework.dto.request.teacher.TeacherGradeCriterionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.request.teacher.TeacherGradeQuestionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.request.teacher.TeacherGradeSubmissionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkRubricCriterionScoreResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkSubmissionAttachmentResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.AiScoringStatusResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherQuestionSubmissionResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionDetailResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionListResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.teacher.TeacherSubmissionStatsResponse;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.GraderType;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkQuestionSubmissionRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRubricCriterionRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkGradedEvent;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkReturnedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherGradingService {

    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkRepository homeworkRepository;
    private final UserRepository userRepository;
    private final HomeworkRubricCriterionRepository criterionRepository;
    private final HomeworkQuestionSubmissionRepository questionSubmissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<TeacherSubmissionListResponse> getSubmissions(Long homeworkId, Long teacherId, HomeworkSubmissionStatus status, Pageable pageable) {
        Long centerId = TenantContext.getCurrentTenantId();

        homeworkRepository.findByIdAndCenter_IdAndTeacher_Id(homeworkId, centerId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found or not owned by teacher."));

        Page<HomeworkSubmission> submissionsPage;
        if (status != null) {
            submissionsPage = submissionRepository.findAllByHomework_IdAndCenter_IdAndStatus(homeworkId, centerId, status, pageable);
        } else {
            submissionsPage = submissionRepository.findAllByHomework_IdAndCenter_Id(homeworkId, centerId, pageable);
        }

        return submissionsPage.map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public TeacherSubmissionStatsResponse getSubmissionStats(Long homeworkId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        homeworkRepository.findByIdAndCenter_IdAndTeacher_Id(homeworkId, centerId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found or not owned by teacher."));

        long total = submissionRepository.countByHomework_IdAndCenter_Id(homeworkId, centerId);
        long graded = submissionRepository.countByHomework_IdAndCenter_IdAndStatusIn(homeworkId, centerId, List.of(HomeworkSubmissionStatus.GRADED));
        long pending = submissionRepository.countByHomework_IdAndCenter_IdAndStatusIn(homeworkId, centerId, List.of(HomeworkSubmissionStatus.SUBMITTED, HomeworkSubmissionStatus.LATE_SUBMITTED));
        long returned = submissionRepository.countByHomework_IdAndCenter_IdAndStatusIn(homeworkId, centerId, List.of(HomeworkSubmissionStatus.RETURNED));

        TeacherSubmissionStatsResponse stats = new TeacherSubmissionStatsResponse();
        stats.setTotalSubmissions(total);
        stats.setGradedCount(graded);
        stats.setPendingCount(pending);
        stats.setReturnedCount(returned);
        
        // Average score logic requires fetching all graded submissions or a native query, simplified here
        stats.setAverageScore(0.0); // Implement proper average via native query if needed

        return stats;
    }

    @Transactional(readOnly = true)
    public TeacherSubmissionDetailResponse getSubmissionDetails(Long submissionId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (!submission.getHomework().getTeacher().getId().equals(teacherId) || !submission.getCenterId().equals(centerId)) {
            throw new ResourceNotFoundException("Submission not found.");
        }

        return mapToDetailResponse(submission);
    }

    @Transactional
    public TeacherSubmissionDetailResponse gradeSubmission(Long submissionId, Long teacherId, TeacherGradeSubmissionRequest request, boolean isDraft) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findWithDetailsByIdAndCenter_IdAndStudent_Id(submissionId, centerId, null) // Generic fetch for teacher
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (!submission.getHomework().getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Submission not found.");
        }

        if (submission.getStatus() == HomeworkSubmissionStatus.IN_PROGRESS || submission.getStatus() == HomeworkSubmissionStatus.RETURNED) {
            throw new BusinessRuleException("Cannot grade an IN_PROGRESS or RETURNED submission.");
        }

        submission.setTeacherFeedback(request.getOverallFeedback());

        Map<Long, HomeworkQuestionSubmission> qsMap = submission.getQuestionSubmissions().stream()
                .collect(Collectors.toMap(HomeworkQuestionSubmission::getId, qs -> qs));

        if (request.getQuestionGrades() != null) {
            for (TeacherGradeQuestionRequest qReq : request.getQuestionGrades()) {
                HomeworkQuestionSubmission qs = qsMap.get(qReq.getQuestionSubmissionId());
                if (qs == null) throw new BusinessRuleException("Question submission not found in this submission.");

                qs.setTeacherFeedback(qReq.getTeacherFeedback());

                // Validate question score
                if (qReq.getScore() != null) {
                    if (qReq.getScore() > qs.getQuestion().getMaxScore()) {
                        throw new BusinessRuleException("Score exceeds maximum for question ID " + qs.getQuestion().getId());
                    }
                    qs.setTeacherOverrideScore(qReq.getScore());
                }

                // Apply Rubric criteria scores
                if (qReq.getCriterionScores() != null) {
                    double totalCriterionScore = 0.0;
                    
                    Map<Long, HomeworkRubricCriterionScore> existingCriteriaScores = qs.getCriterionScores().stream()
                            .filter(cs -> cs.getGraderType() == GraderType.TEACHER)
                            .collect(Collectors.toMap(cs -> cs.getCriterion().getId(), cs -> cs));

                    for (TeacherGradeCriterionRequest cReq : qReq.getCriterionScores()) {
                        HomeworkRubricCriterion criterion = criterionRepository.findById(cReq.getCriterionId())
                                .orElseThrow(() -> new BusinessRuleException("Criterion not found"));

                        if (cReq.getScore() > criterion.getMaxScore()) {
                            throw new BusinessRuleException("Criterion score exceeds maximum for criterion ID " + criterion.getId());
                        }

                        totalCriterionScore += cReq.getScore();

                        HomeworkRubricCriterionScore cs = existingCriteriaScores.get(criterion.getId());
                        if (cs == null) {
                            cs = HomeworkRubricCriterionScore.builder()
                                    .questionSubmission(qs)
                                    .criterion(criterion)
                                    .graderType(GraderType.TEACHER)
                                    .build();
                            qs.getCriterionScores().add(cs);
                        }
                        cs.setScore(cReq.getScore());
                        cs.setComment(cReq.getComment());
                    }

                    if (qReq.getScore() == null) {
                        // Auto-sum criterion scores if override score isn't explicitly provided
                        if (totalCriterionScore > qs.getQuestion().getMaxScore()) {
                             throw new BusinessRuleException("Total rubric score exceeds maximum for question ID " + qs.getQuestion().getId());
                        }
                        qs.setTeacherOverrideScore(totalCriterionScore);
                    }
                }
            }
        }

        if (isDraft) {
            submission.setStatus(HomeworkSubmissionStatus.GRADING_IN_PROGRESS);
        } else {
            // Need to determine oldScore. 
            // In Epic 5 we didn't track oldScore natively in the DB, but we know if it was GRADED before.
            Double oldScore = null;
            if (submission.getStatus() == HomeworkSubmissionStatus.GRADED) {
                 double total = 0.0;
                 for (var qs : submission.getQuestionSubmissions()) {
                     if (qs.getTeacherOverrideScore() != null) total += qs.getTeacherOverrideScore();
                     else if (qs.getScore() != null) total += qs.getScore();
                 }
                 oldScore = total;
            }
            
            submission.setStatus(HomeworkSubmissionStatus.GRADED);
            submission.setGradedAt(Instant.now());
            User teacher = userRepository.findById(teacherId).orElseThrow();
            submission.setGradedBy(teacher);
            
            // Calculate new score after the updates were applied in this method
            double newScore = 0.0;
            for (var qs : submission.getQuestionSubmissions()) {
                 if (qs.getTeacherOverrideScore() != null) newScore += qs.getTeacherOverrideScore();
                 else if (qs.getScore() != null) newScore += qs.getScore();
            }
            
            // Fire Analytics Event
            eventPublisher.publishEvent(new HomeworkGradedEvent(
                submission.getHomework().getId(),
                submission.getHomework().getClazz().getId(),
                submission.getCenterId(),
                submission.getStudent().getId(),
                oldScore,
                newScore
            ));
        }

        submission = submissionRepository.save(submission);
        return mapToDetailResponse(submission);
    }
    
    @Transactional
    public void returnSubmission(Long submissionId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (!submission.getHomework().getTeacher().getId().equals(teacherId) || !submission.getCenterId().equals(centerId)) {
            throw new ResourceNotFoundException("Submission not found.");
        }
        
        if (submission.getStatus() == HomeworkSubmissionStatus.GRADED) {
             double oldScore = 0.0;
             for (var qs : submission.getQuestionSubmissions()) {
                 if (qs.getTeacherOverrideScore() != null) oldScore += qs.getTeacherOverrideScore();
                 else if (qs.getScore() != null) oldScore += qs.getScore();
             }
             eventPublisher.publishEvent(new HomeworkReturnedEvent(
                 submission.getHomework().getId(),
                 submission.getHomework().getClazz().getId(),
                 submission.getCenterId(),
                 submission.getStudent().getId(),
                 oldScore
             ));
        }

        submission.setStatus(HomeworkSubmissionStatus.RETURNED);
        submissionRepository.save(submission);
    }

    /**
     * Returns the AI scoring status for each question submission in a homework submission.
     * Only teachers who own the homework can call this.
     */
    @Transactional(readOnly = true)
    public List<AiScoringStatusResponse> getAiScoringStatus(Long submissionId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (!submission.getHomework().getTeacher().getId().equals(teacherId)
                || !submission.getCenterId().equals(centerId)) {
            throw new ResourceNotFoundException("Submission not found.");
        }

        return submission.getQuestionSubmissions().stream().map(qs -> {
            AiScoringStatusResponse r = new AiScoringStatusResponse();
            r.setQuestionSubmissionId(qs.getId());
            r.setAiScoringStatus(qs.getAiScoringStatus());
            r.setAiScoredAt(qs.getAiScoredAt());
            return r;
        }).collect(Collectors.toList());
    }

    private TeacherSubmissionListResponse mapToListResponse(HomeworkSubmission s) {
        TeacherSubmissionListResponse r = new TeacherSubmissionListResponse();
        r.setId(s.getId());
        r.setStudentId(s.getStudent().getId());
        r.setStudentName(s.getStudent().getFullName());
        r.setStudentEmail(s.getStudent().getEmail());
        r.setAttemptNumber(s.getAttemptNumber());
        r.setStatus(s.getStatus());
        r.setSubmittedAt(s.getSubmittedAt());
        
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

    private TeacherSubmissionDetailResponse mapToDetailResponse(HomeworkSubmission s) {
        TeacherSubmissionDetailResponse r = new TeacherSubmissionDetailResponse();
        r.setId(s.getId());
        r.setHomeworkId(s.getHomework().getId());
        r.setStudentId(s.getStudent().getId());
        r.setStudentName(s.getStudent().getFullName());
        r.setAttemptNumber(s.getAttemptNumber());
        r.setStatus(s.getStatus());
        r.setSubmittedAt(s.getSubmittedAt());
        r.setGradedAt(s.getGradedAt());
        r.setTeacherFeedback(s.getTeacherFeedback());

        double total = 0.0;
        List<TeacherQuestionSubmissionResponse> qResponses = new ArrayList<>();
        
        for (HomeworkQuestionSubmission qs : s.getQuestionSubmissions()) {
            TeacherQuestionSubmissionResponse qRes = new TeacherQuestionSubmissionResponse();
            qRes.setId(qs.getId());
            qRes.setQuestionId(qs.getQuestion().getId());
            qRes.setTextAnswer(qs.getTextAnswer());
            qRes.setAutoScore(qs.getScore());
            qRes.setTeacherOverrideScore(qs.getTeacherOverrideScore());
            
            Double effectiveScore = qs.getTeacherOverrideScore() != null ? qs.getTeacherOverrideScore() : (qs.getScore() != null ? qs.getScore() : 0.0);
            qRes.setEffectiveScore(effectiveScore);
            total += effectiveScore;
            
            qRes.setIsCorrect(qs.getIsCorrect());
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
                    StudentHomeworkRubricCriterionScoreResponse csRes = new StudentHomeworkRubricCriterionScoreResponse();
                    csRes.setId(cs.getId());
                    csRes.setCriterionId(cs.getCriterion().getId());
                    csRes.setScore(cs.getScore());
                    csRes.setComment(cs.getComment());
                    csRes.setGraderType(cs.getGraderType());
                    return csRes;
                }).collect(Collectors.toList()));
            }
            qResponses.add(qRes);
        }
        
        r.setQuestionSubmissions(qResponses);
        r.setTotalScore(total);
        return r;
    }
}
