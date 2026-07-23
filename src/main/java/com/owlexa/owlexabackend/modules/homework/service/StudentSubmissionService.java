package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.homework.dto.request.student.AutosaveQuestionSubmissionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.request.student.AutosaveSubmissionRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.*;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkStatus;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkQuestionRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkGradedEvent;
import com.owlexa.owlexabackend.modules.analytics.event.HomeworkSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentSubmissionService {

    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkRepository homeworkRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final HomeworkQuestionRepository questionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StudentHomeworkSubmissionResponse getOrCreateAttempt(Long homeworkId, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        Homework homework = homeworkRepository.findWithDetailsByIdAndCenter_Id(homeworkId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found."));

        if (homework.getStatus() == HomeworkStatus.DRAFT) {
            throw new ResourceNotFoundException("Homework not found.");
        }

        boolean isEnrolled = classEnrollmentRepository.existsByClazz_IdAndStudentUser_IdAndStatus(
                homework.getClazz().getId(), studentId, EnrollmentStatus.ACTIVE);
        if (!isEnrolled) {
            throw new ResourceNotFoundException("Access denied.");
        }

        Optional<HomeworkSubmission> activeAttemptOpt = submissionRepository.findFirstByHomework_IdAndCenter_IdAndStudent_IdAndStatusOrderByAttemptNumberDesc(
                homeworkId, centerId, studentId, HomeworkSubmissionStatus.IN_PROGRESS);

        if (activeAttemptOpt.isPresent()) {
            return mapToResponse(activeAttemptOpt.get());
        }

        // If no active, check if latest exists to handle clone
        Optional<HomeworkSubmission> latestAttemptOpt = submissionRepository.findFirstByHomework_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(
                homeworkId, centerId, studentId);

        if (latestAttemptOpt.isPresent()) {
            HomeworkSubmission latest = latestAttemptOpt.get();
            if (Boolean.FALSE.equals(homework.getAllowResubmit())) {
                throw new BusinessRuleException("Resubmission is not allowed for this homework.");
            }
            // Clone attempt
            HomeworkSubmission newAttempt = cloneSubmission(latest);
            newAttempt.setAttemptNumber(latest.getAttemptNumber() + 1);
            newAttempt = submissionRepository.save(newAttempt);
            return mapToResponse(newAttempt);
        }

        // Create new Attempt 1
        User student = userRepository.findById(studentId).orElseThrow();
        HomeworkSubmission newAttempt = HomeworkSubmission.builder()
                .homework(homework)
                .student(student)
                .center(homework.getCenter())
                .attemptNumber(1)
                .status(HomeworkSubmissionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .questionSubmissions(new ArrayList<>())
                .build();

        newAttempt = submissionRepository.save(newAttempt);
        return mapToResponse(newAttempt);
    }

    private HomeworkSubmission cloneSubmission(HomeworkSubmission source) {
        HomeworkSubmission clone = HomeworkSubmission.builder()
                .homework(source.getHomework())
                .student(source.getStudent())
                .center(source.getCenter())
                .status(HomeworkSubmissionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .questionSubmissions(new ArrayList<>())
                .build();

        for (HomeworkQuestionSubmission qs : source.getQuestionSubmissions()) {
            HomeworkQuestionSubmission qsClone = HomeworkQuestionSubmission.builder()
                    .submission(clone)
                    .question(qs.getQuestion())
                    .textAnswer(qs.getTextAnswer())
                    .build();

            for (HomeworkSubmissionAttachment att : qs.getAttachments()) {
                qsClone.getAttachments().add(HomeworkSubmissionAttachment.builder()
                        .questionSubmission(qsClone)
                        .fileUrl(att.getFileUrl())
                        .fileName(att.getFileName())
                        .fileType(att.getFileType())
                        .build());
            }

            for (HomeworkQuestionSubmissionOption opt : qs.getSelectedOptions()) {
                qsClone.getSelectedOptions().add(HomeworkQuestionSubmissionOption.builder()
                        .questionSubmission(qsClone)
                        .option(opt.getOption())
                        .build());
            }
            clone.getQuestionSubmissions().add(qsClone);
        }

        return clone;
    }

    @Transactional
    public StudentHomeworkSubmissionResponse autosave(Long submissionId, AutosaveSubmissionRequest request, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findWithDetailsByIdAndCenter_IdAndStudent_Id(submissionId, centerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (submission.getStatus() != HomeworkSubmissionStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Can only autosave IN_PROGRESS attempts.");
        }
        
        if (!submission.getVersion().equals(request.getVersion())) {
             throw new BusinessRuleException("Optimistic locking failure: submission has been updated by another request. Please refresh.");
        }

        submission.getQuestionSubmissions().clear(); // Clear to rebuild
        
        // This is a naive rebuild for autosave. In a prod app, we might diff, but for autosave of tree it's okay.
        if (request.getQuestionSubmissions() != null) {
             for (AutosaveQuestionSubmissionRequest qsReq : request.getQuestionSubmissions()) {
                 HomeworkQuestion question = questionRepository.findById(qsReq.getQuestionId())
                         .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
                         
                 HomeworkQuestionSubmission qs = HomeworkQuestionSubmission.builder()
                         .submission(submission)
                         .question(question)
                         .textAnswer(qsReq.getTextAnswer())
                         .build();
                         
                 if (qsReq.getAttachments() != null) {
                     for (var attReq : qsReq.getAttachments()) {
                         qs.getAttachments().add(HomeworkSubmissionAttachment.builder()
                                 .questionSubmission(qs)
                                 .fileUrl(attReq.getFileUrl())
                                 .fileName(attReq.getFileName())
                                 .fileType(attReq.getFileType())
                                 .build());
                     }
                 }
                 
                 if (qsReq.getSelectedOptionIds() != null && question.getOptions() != null) {
                     for (Long optId : qsReq.getSelectedOptionIds()) {
                         HomeworkQuestionOption option = question.getOptions().stream()
                                 .filter(o -> o.getId().equals(optId))
                                 .findFirst()
                                 .orElseThrow(() -> new BusinessRuleException("Invalid option id"));
                                 
                         qs.getSelectedOptions().add(HomeworkQuestionSubmissionOption.builder()
                                 .questionSubmission(qs)
                                 .option(option)
                                 .build());
                     }
                 }
                 
                 submission.getQuestionSubmissions().add(qs);
             }
        }

        submission.setLastSavedAt(Instant.now());
        submission = submissionRepository.save(submission);
        return mapToResponse(submission);
    }

    @Transactional
    public StudentHomeworkSubmissionResponse submit(Long submissionId, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkSubmission submission = submissionRepository.findWithDetailsByIdAndCenter_IdAndStudent_Id(submissionId, centerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found."));

        if (submission.getStatus() != HomeworkSubmissionStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Submission is already finalized.");
        }

        Instant now = Instant.now();
        Homework homework = submission.getHomework();
        
        if (homework.getDueDate() != null && now.isAfter(homework.getDueDate())) {
            if (Boolean.FALSE.equals(homework.getAllowLateSubmission())) {
                throw new BusinessRuleException("Late submissions are not allowed.");
            }
            submission.setStatus(HomeworkSubmissionStatus.LATE_SUBMITTED);
        } else {
            submission.setStatus(HomeworkSubmissionStatus.SUBMITTED);
        }

        submission.setSubmittedAt(now);

        // Auto-grade Quizzes
        for (HomeworkQuestionSubmission qs : submission.getQuestionSubmissions()) {
            if (qs.getQuestion().getType() == HomeworkQuestionType.QUIZ) {
                // Determine correctness: all correct options must be selected, and no incorrect ones
                List<Long> correctOptionIds = qs.getQuestion().getOptions().stream()
                        .filter(HomeworkQuestionOption::getIsCorrect)
                        .map(HomeworkQuestionOption::getId)
                        .collect(Collectors.toList());
                        
                List<Long> selectedOptionIds = qs.getSelectedOptions().stream()
                        .map(so -> so.getOption().getId())
                        .collect(Collectors.toList());
                        
                boolean isCorrect = correctOptionIds.size() == selectedOptionIds.size() && 
                                    correctOptionIds.containsAll(selectedOptionIds);
                                    
                qs.setIsCorrect(isCorrect);
                qs.setScore(isCorrect ? qs.getQuestion().getMaxScore() : 0.0);
            }
        }

        submission = submissionRepository.save(submission);
        
        eventPublisher.publishEvent(new HomeworkSubmittedEvent(
            homework.getId(),
            homework.getClazz().getId(),
            submission.getCenterId(),
            studentId,
            submission.getStatus() == HomeworkSubmissionStatus.LATE_SUBMITTED,
            submission.getId()
        ));
        
        return mapToResponse(submission);
    }
    
    @Transactional(readOnly = true)
    public StudentHomeworkSubmissionResponse getActiveAttempt(Long homeworkId, Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkSubmission submission = submissionRepository.findFirstByHomework_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(homeworkId, centerId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found."));
        return mapToResponse(submission);
    }

    private StudentHomeworkSubmissionResponse mapToResponse(HomeworkSubmission submission) {
        StudentHomeworkSubmissionResponse response = new StudentHomeworkSubmissionResponse();
        response.setId(submission.getId());
        response.setHomeworkId(submission.getHomework().getId());
        response.setAttemptNumber(submission.getAttemptNumber());
        response.setStatus(submission.getStatus());
        response.setStartedAt(submission.getStartedAt());
        response.setLastSavedAt(submission.getLastSavedAt());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setGradedAt(submission.getGradedAt());
        response.setTeacherFeedback(submission.getTeacherFeedback());
        response.setVersion(submission.getVersion());
        
        double totalScore = 0.0;
        
        if (submission.getQuestionSubmissions() != null) {
            List<StudentHomeworkQuestionSubmissionResponse> qResponses = new ArrayList<>();
            for (HomeworkQuestionSubmission qs : submission.getQuestionSubmissions()) {
                StudentHomeworkQuestionSubmissionResponse qRes = new StudentHomeworkQuestionSubmissionResponse();
                qRes.setId(qs.getId());
                qRes.setQuestionId(qs.getQuestion().getId());
                qRes.setTextAnswer(qs.getTextAnswer());
                qRes.setScore(qs.getScore());
                qRes.setIsCorrect(qs.getIsCorrect());
                qRes.setTeacherFeedback(qs.getTeacherFeedback());
                qRes.setAiFeedback(qs.getAiFeedback());
                
                if (qs.getScore() != null) {
                    totalScore += qs.getScore();
                }
                
                if (qs.getAttachments() != null) {
                    qRes.setAttachments(qs.getAttachments().stream().map(a -> {
                        StudentHomeworkSubmissionAttachmentResponse aRes = new StudentHomeworkSubmissionAttachmentResponse();
                        aRes.setId(a.getId());
                        aRes.setFileName(a.getFileName());
                        aRes.setFileUrl(a.getFileUrl());
                        aRes.setFileType(a.getFileType());
                        return aRes;
                    }).collect(Collectors.toList()));
                }
                
                if (qs.getSelectedOptions() != null) {
                    qRes.setSelectedOptionIds(qs.getSelectedOptions().stream()
                            .map(o -> o.getOption().getId())
                            .collect(Collectors.toList()));
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
            response.setQuestionSubmissions(qResponses);
        }
        
        response.setTotalScore(totalScore);
        
        return response;
    }
}
