package com.owlexa.owlexabackend.modules.teacher_review.mapper;

import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.StudentReviewItemResultResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.StudentReviewResultResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewDetailResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewItemResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewSummaryResponse;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReview;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewItem;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TeacherReviewMapper {

    public TeacherReviewDetailResponse toDetailResponse(TeacherReview review) {
        return TeacherReviewDetailResponse.builder()
                .id(review.getId())
                .submissionAttemptId(review.getSubmissionAttempt().getId())
                .selectedAiGradingResultId(review.getSelectedAiGradingResult() == null
                        ? null
                        : review.getSelectedAiGradingResult().getId())
                .status(review.getStatus())
                .overallComment(review.getOverallComment())
                .autoScore(review.getSubmissionAttempt().getAutoScore())
                .finalScore(review.getFinalScore())
                .maxScore(review.getMaxScore())
                .version(review.getVersion())
                .items(toTeacherItemResponses(review.getItems()))
                .createdByUserId(userId(review.getCreatedBy()))
                .createdByFullName(userFullName(review.getCreatedBy()))
                .updatedByUserId(userId(review.getUpdatedBy()))
                .updatedByFullName(userFullName(review.getUpdatedBy()))
                .finalizedByUserId(userId(review.getFinalizedBy()))
                .finalizedByFullName(userFullName(review.getFinalizedBy()))
                .finalizedAt(review.getFinalizedAt())
                .releasedByUserId(userId(review.getReleasedBy()))
                .releasedByFullName(userFullName(review.getReleasedBy()))
                .releasedAt(review.getReleasedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    public TeacherReviewSummaryResponse toSummaryResponse(
            SubmissionAttempt attempt,
            TeacherReview review,
            boolean hasEssay,
            boolean hasAiResult
    ) {
        AssignmentRecipient recipient = attempt.getAssignmentRecipient();
        Assignment assignment = recipient.getAssignment();
        User student = recipient.getStudentUser();
        com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = recipient.getClazz();

        return TeacherReviewSummaryResponse.builder()
                .submissionAttemptId(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentRecipientId(recipient.getId())
                .studentUserId(student.getId())
                .studentFullName(student.getFullName())
                .classId(clazz == null ? null : clazz.getId())
                .className(clazz == null ? null : clazz.getName())
                .attemptNumber(attempt.getAttemptNumber())
                .submissionStatus(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .reviewId(review == null ? null : review.getId())
                .reviewStatus(review == null ? null : review.getStatus())
                .hasEssay(hasEssay)
                .hasAiResult(hasAiResult)
                .selectedAiGradingResultId(selectedAiGradingResultId(review))
                .autoScore(attempt.getAutoScore())
                .finalScore(review == null ? null : review.getFinalScore())
                .maxScore(review == null ? attempt.getMaxScore() : review.getMaxScore())
                .build();
    }

    public StudentReviewResultResponse toStudentResultResponse(TeacherReview review) {
        SubmissionAttempt attempt = review.getSubmissionAttempt();
        return StudentReviewResultResponse.builder()
                .submissionAttemptId(attempt.getId())
                .assignmentTitleSnapshot(attempt.getAssignmentTitleSnapshot())
                .assignmentTypeSnapshot(attempt.getAssignmentTypeSnapshot())
                .attemptNumber(attempt.getAttemptNumber())
                .finalScore(review.getFinalScore())
                .maxScore(review.getMaxScore())
                .overallComment(review.getOverallComment())
                .releasedAt(review.getReleasedAt())
                .essayItems(toStudentItemResponses(review.getItems()))
                .build();
    }

    private List<TeacherReviewItemResponse> toTeacherItemResponses(List<TeacherReviewItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(TeacherReviewItem::getDisplayOrderSnapshot))
                .map(this::toTeacherItemResponse)
                .toList();
    }

    private TeacherReviewItemResponse toTeacherItemResponse(TeacherReviewItem item) {
        return TeacherReviewItemResponse.builder()
                .id(item.getId())
                .assignmentItemId(item.getAssignmentItem().getId())
                .submissionAnswerId(item.getSubmissionAnswer() == null
                        ? null
                        : item.getSubmissionAnswer().getId())
                .questionTitleSnapshot(item.getQuestionTitleSnapshot())
                .displayOrderSnapshot(item.getDisplayOrderSnapshot())
                .finalScore(item.getFinalScore())
                .maxScore(item.getMaxScore())
                .itemComment(item.getItemComment())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private List<StudentReviewItemResultResponse> toStudentItemResponses(List<TeacherReviewItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(TeacherReviewItem::getDisplayOrderSnapshot))
                .map(item -> StudentReviewItemResultResponse.builder()
                        .assignmentItemId(item.getAssignmentItem().getId())
                        .questionTitle(item.getQuestionTitleSnapshot())
                        .displayOrder(item.getDisplayOrderSnapshot())
                        .finalScore(item.getFinalScore())
                        .maxScore(item.getMaxScore())
                        .teacherComment(item.getItemComment())
                        .build())
                .toList();
    }

    private Long selectedAiGradingResultId(TeacherReview review) {
        return review == null || review.getSelectedAiGradingResult() == null
                ? null
                : review.getSelectedAiGradingResult().getId();
    }

    private Long userId(User user) {
        return user == null ? null : user.getId();
    }

    private String userFullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
