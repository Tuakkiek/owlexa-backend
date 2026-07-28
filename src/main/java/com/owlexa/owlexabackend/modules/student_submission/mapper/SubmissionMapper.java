package com.owlexa.owlexabackend.modules.student_submission.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAnswerResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAttemptItemOptionResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherSubmissionSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswerOption;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubmissionMapper {

    private final RichTextDocumentService richTextDocumentService;
    private final FileMapper fileMapper;

    public StudentAttemptSummaryResponse toStudentAttemptSummaryResponse(SubmissionAttempt attempt) {
        return StudentAttemptSummaryResponse.builder()
                .id(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .lastSavedAt(attempt.getLastSavedAt())
                .submittedAt(attempt.getSubmittedAt())
                .autoScore(attempt.getAutoScore())
                .maxScore(attempt.getMaxScore())
                .build();
    }

    public StudentAttemptDetailResponse toStudentAttemptDetailResponse(SubmissionAttempt attempt) {
        AssignmentRecipient recipient = attempt.getAssignmentRecipient();
        Assignment assignment = recipient.getAssignment();
        return StudentAttemptDetailResponse.builder()
                .id(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentRecipientId(recipient.getId())
                .assignmentTitleSnapshot(attempt.getAssignmentTitleSnapshot())
                .assignmentTypeSnapshot(attempt.getAssignmentTypeSnapshot())
                .status(attempt.getStatus())
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .lastSavedAt(attempt.getLastSavedAt())
                .submittedAt(attempt.getSubmittedAt())
                .autoScore(attempt.getAutoScore())
                .maxScore(attempt.getMaxScore())
                .audioFile(toFileResponse(assignment.getAudioFile()))
                .playbackMode(assignment.getPlaybackMode())
                .items(toItemResponses(assignment.getItems()))
                .answers(toAnswerResponses(attempt.getAnswers()))
                .build();
    }

    public TeacherSubmissionSummaryResponse toTeacherSubmissionSummaryResponse(
            AssignmentRecipient recipient,
            SubmissionAttempt latestAttempt,
            long attemptsCount
    ) {
        User student = recipient.getStudentUser();
        com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = recipient.getClazz();

        return TeacherSubmissionSummaryResponse.builder()
                .recipientId(recipient.getId())
                .studentUserId(student.getId())
                .studentFullName(student.getFullName())
                .classId(clazz == null ? null : clazz.getId())
                .className(clazz == null ? null : clazz.getName())
                .sourceType(recipient.getSourceType())
                .recipientStatus(recipient.getStatus())
                .latestAttemptId(latestAttempt == null ? null : latestAttempt.getId())
                .latestAttemptNumber(latestAttempt == null ? null : latestAttempt.getAttemptNumber())
                .latestStatus(latestAttempt == null ? null : latestAttempt.getStatus())
                .latestStartedAt(latestAttempt == null ? null : latestAttempt.getStartedAt())
                .latestSubmittedAt(latestAttempt == null ? null : latestAttempt.getSubmittedAt())
                .latestAutoScore(latestAttempt == null ? null : latestAttempt.getAutoScore())
                .maxScore(latestAttempt == null ? null : latestAttempt.getMaxScore())
                .attemptsCount(attemptsCount)
                .build();
    }

    public TeacherAttemptDetailResponse toTeacherAttemptDetailResponse(SubmissionAttempt attempt) {
        AssignmentRecipient recipient = attempt.getAssignmentRecipient();
        Assignment assignment = recipient.getAssignment();
        User student = recipient.getStudentUser();
        com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = recipient.getClazz();

        return TeacherAttemptDetailResponse.builder()
                .id(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentRecipientId(recipient.getId())
                .studentUserId(student.getId())
                .studentFullName(student.getFullName())
                .classId(clazz == null ? null : clazz.getId())
                .className(clazz == null ? null : clazz.getName())
                .sourceType(recipient.getSourceType())
                .recipientStatus(recipient.getStatus())
                .assignmentTitleSnapshot(attempt.getAssignmentTitleSnapshot())
                .assignmentTypeSnapshot(attempt.getAssignmentTypeSnapshot())
                .status(attempt.getStatus())
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .lastSavedAt(attempt.getLastSavedAt())
                .submittedAt(attempt.getSubmittedAt())
                .autoScore(attempt.getAutoScore())
                .maxScore(attempt.getMaxScore())
                .items(toItemResponses(assignment.getItems()))
                .answers(toAnswerResponses(attempt.getAnswers()))
                .build();
    }

    private List<SubmissionAttemptItemResponse> toItemResponses(List<AssignmentItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(AssignmentItem::getDisplayOrder))
                .map(this::toItemResponse)
                .toList();
    }

    private SubmissionAttemptItemResponse toItemResponse(AssignmentItem item) {
        return SubmissionAttemptItemResponse.builder()
                .assignmentItemId(item.getId())
                .questionType(item.getQuestionType())
                .title(item.getTitle())
                .content(richTextDocumentService.deserialize(item.getContentJson()))
                .difficulty(item.getDifficulty())
                .points(item.getPoints())
                .explanation(richTextDocumentService.deserializeOptional(item.getExplanationJson()))
                .sampleAnswer(richTextDocumentService.deserializeOptional(item.getSampleAnswerJson()))
                .gradingCriteriaName(item.getGradingCriteriaName())
                .gradingCriteriaContent(richTextDocumentService.deserializeOptional(item.getGradingCriteriaContentJson()))
                .displayOrder(item.getDisplayOrder())
                .options(toOptionResponses(item.getOptions()))
                .build();
    }

    private List<SubmissionAttemptItemOptionResponse> toOptionResponses(List<AssignmentItemOption> options) {
        return options.stream()
                .sorted(Comparator.comparing(AssignmentItemOption::getDisplayOrder))
                .map(option -> SubmissionAttemptItemOptionResponse.builder()
                        .assignmentItemOptionId(option.getId())
                        .content(option.getContent())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }

    private List<SubmissionAnswerResponse> toAnswerResponses(List<SubmissionAnswer> answers) {
        return answers.stream()
                .sorted(Comparator.comparing(answer -> answer.getAssignmentItem().getDisplayOrder()))
                .map(this::toAnswerResponse)
                .toList();
    }

    private SubmissionAnswerResponse toAnswerResponse(SubmissionAnswer answer) {
        return SubmissionAnswerResponse.builder()
                .assignmentItemId(answer.getAssignmentItem().getId())
                .answerText(answer.getAnswerText())
                .selectedOptionIds(toSelectedOptionIds(answer.getSelectedOptions()))
                .autoScore(answer.getAutoScore())
                .maxScore(answer.getMaxScore())
                .gradedAt(answer.getGradedAt())
                .build();
    }

    private List<Long> toSelectedOptionIds(List<SubmissionAnswerOption> selectedOptions) {
        return selectedOptions.stream()
                .sorted(Comparator.comparing(option -> option.getAssignmentItemOption().getDisplayOrder()))
                .map(option -> option.getAssignmentItemOption().getId())
                .toList();
    }

    private FileResponse toFileResponse(StoredFile file) {
        return file == null ? null : fileMapper.toResponse(file);
    }
}
