package com.owlexa.owlexabackend.modules.student_submission.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;

import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentBlockResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAnswerResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAttemptItemOptionResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherSubmissionSummaryResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswerOption;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
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
                .displayedScore(attempt.getAutoScore())
                .maxScore(attempt.getMaxScore())
                .build();
    }

    public StudentAttemptDetailResponse toStudentAttemptDetailResponse(SubmissionAttempt attempt) {
        AssignmentRecipient recipient = attempt.getAssignmentRecipient();
        Assignment assignment = recipient.getAssignment();

        boolean showScore = assignment.getShowScore() == null || assignment.getShowScore();
        boolean allowReview = assignment.getAllowReview() == null || assignment.getAllowReview();
        boolean hasPassword = assignment.getAccessPassword() != null && !assignment.getAccessPassword().isBlank();

        return StudentAttemptDetailResponse.builder()
                .id(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentRecipientId(recipient.getId())
                .assignmentTitleSnapshot(attempt.getAssignmentTitleSnapshot())

                .assignmentContent(richTextDocumentService.deserialize(assignment.getContentJson()))
                .status(attempt.getStatus())
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .lastSavedAt(attempt.getLastSavedAt())
                .submittedAt(attempt.getSubmittedAt())
                .autoScore(showScore ? attempt.getAutoScore() : null)
                .maxScore(showScore ? attempt.getMaxScore() : null)
                .audioFile(toFileResponse(assignment.getAudioFile()))
                .playbackMode(assignment.getPlaybackMode())
                .items(toStudentItemResponses(assignment.getItems()))
                .answers((allowReview || attempt.getStatus() == SubmissionAttemptStatus.IN_PROGRESS) ? toAnswerResponses(attempt.getAnswers()) : List.of())
                .blocks(toBlockResponses(assignment.getBlocks()))
                .showScore(showScore)
                .allowReview(allowReview)
                .hasPassword(hasPassword)
                .build();
    }

    public TeacherSubmissionSummaryResponse toTeacherSubmissionSummaryResponse(
            AssignmentRecipient recipient,
            SubmissionAttempt latestAttempt,
            long attemptsCount,
            BigDecimal latestFinalScore,
            Boolean isGraded
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
                .latestFinalScore(latestFinalScore)
                .isGraded(isGraded)
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

                .assignmentContent(richTextDocumentService.deserialize(assignment.getContentJson()))
                .status(attempt.getStatus())
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .lastSavedAt(attempt.getLastSavedAt())
                .submittedAt(attempt.getSubmittedAt())
                .autoScore(attempt.getAutoScore())
                .maxScore(attempt.getMaxScore())
                .items(toItemResponses(assignment.getItems()))
                .answers(toAnswerResponses(attempt.getAnswers()))
                .blocks(toBlockResponses(assignment.getBlocks()))
                .build();
    }

    private List<AssignmentBlockResponse> toBlockResponses(
            List<com.owlexa.owlexabackend.modules.assignment.entity.AssignmentContentBlock> blocks
    ) {
        if (blocks == null) return List.of();
        return blocks.stream()
                .sorted(Comparator.comparing(com.owlexa.owlexabackend.modules.assignment.entity.AssignmentContentBlock::getPosition))
                .map(block -> AssignmentBlockResponse.builder()
                        .id(block.getId())
                        .position(block.getPosition())
                        .title(block.getTitle())
                        .content(richTextDocumentService.deserialize(block.getContentJson()))
                        .build())
                .toList();
    }

    private List<SubmissionAttemptItemResponse> toItemResponses(List<AssignmentItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(AssignmentItem::getDisplayOrder))
                .map(this::toItemResponse)
                .toList();
    }

    private List<StudentAttemptItemResponse> toStudentItemResponses(List<AssignmentItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(AssignmentItem::getDisplayOrder))
                .map(this::toStudentItemResponse)
                .toList();
    }

    private StudentAttemptItemResponse toStudentItemResponse(AssignmentItem item) {
        return StudentAttemptItemResponse.builder()
                .assignmentItemId(item.getId())
                .questionType(item.getQuestionType())
                .title(item.getTitle())
                .content(richTextDocumentService.deserialize(item.getContentJson()))
                .difficulty(item.getDifficulty())
                .points(item.getPoints())
                .displayOrder(item.getDisplayOrder())
                .options(toOptionResponses(item.getOptions()))
                .build();
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
