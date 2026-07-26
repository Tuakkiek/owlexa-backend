package com.owlexa.owlexabackend.modules.assignment.mapper;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentItemOptionResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentItemResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentRecipientResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentTargetResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTarget;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AssignmentMapper {

    public AssignmentListResponse toListResponse(Assignment assignment) {
        return AssignmentListResponse.builder()
                .id(assignment.getId())
                .assessmentId(assignment.getAssessment().getId())
                .type(assignment.getType())
                .status(assignment.getStatus())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .openAt(assignment.getOpenAt())
                .dueAt(assignment.getDueAt())
                .attemptLimit(assignment.getAttemptLimit())
                .assessmentSnapshotAt(assignment.getAssessmentSnapshotAt())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    public AssignmentDetailResponse toDetailResponse(Assignment assignment) {
        return AssignmentDetailResponse.builder()
                .id(assignment.getId())
                .assessmentId(assignment.getAssessment().getId())
                .type(assignment.getType())
                .status(assignment.getStatus())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .openAt(assignment.getOpenAt())
                .dueAt(assignment.getDueAt())
                .attemptLimit(assignment.getAttemptLimit())
                .assessmentSnapshotAt(assignment.getAssessmentSnapshotAt())
                .targets(toTargetResponses(assignment.getTargets()))
                .recipients(toRecipientResponses(assignment.getRecipients()))
                .items(toItemResponses(assignment.getItems()))
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    public StudentAssignmentListResponse toStudentListResponse(AssignmentRecipient recipient) {
        Assignment assignment = recipient.getAssignment();
        return StudentAssignmentListResponse.builder()
                .id(assignment.getId())
                .recipientId(recipient.getId())
                .type(assignment.getType())
                .status(assignment.getStatus())
                .recipientStatus(recipient.getStatus())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .openAt(assignment.getOpenAt())
                .dueAt(assignment.getDueAt())
                .attemptLimit(assignment.getAttemptLimit())
                .assignedAt(recipient.getAssignedAt())
                .build();
    }

    public StudentAssignmentDetailResponse toStudentDetailResponse(AssignmentRecipient recipient) {
        Assignment assignment = recipient.getAssignment();
        return StudentAssignmentDetailResponse.builder()
                .id(assignment.getId())
                .recipientId(recipient.getId())
                .type(assignment.getType())
                .status(assignment.getStatus())
                .recipientStatus(recipient.getStatus())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .openAt(assignment.getOpenAt())
                .dueAt(assignment.getDueAt())
                .attemptLimit(assignment.getAttemptLimit())
                .assignedAt(recipient.getAssignedAt())
                .items(toItemResponses(assignment.getItems()))
                .build();
    }

    public AssignmentItem toItemSnapshot(AssessmentItem assessmentItem) {
        AssignmentItem item = AssignmentItem.builder()
                .assessmentItem(assessmentItem)
                .questionType(assessmentItem.getQuestionType())
                .title(assessmentItem.getTitle())
                .content(assessmentItem.getContent())
                .difficulty(assessmentItem.getDifficulty())
                .points(assessmentItem.getPoints())
                .explanation(assessmentItem.getExplanation())
                .sampleAnswer(assessmentItem.getSampleAnswer())
                .gradingCriteriaName(assessmentItem.getGradingCriteriaName())
                .gradingCriteriaContent(assessmentItem.getGradingCriteriaContent())
                .displayOrder(assessmentItem.getDisplayOrder())
                .build();

        assessmentItem.getOptions().stream()
                .sorted(Comparator.comparing(AssessmentItemOption::getDisplayOrder))
                .map(this::toOptionSnapshot)
                .forEach(option -> {
                    option.setAssignmentItem(item);
                    item.getOptions().add(option);
                });

        return item;
    }

    private AssignmentItemOption toOptionSnapshot(AssessmentItemOption option) {
        return AssignmentItemOption.builder()
                .content(option.getContent())
                .isCorrect(option.getIsCorrect())
                .displayOrder(option.getDisplayOrder())
                .build();
    }

    private List<AssignmentTargetResponse> toTargetResponses(List<AssignmentTarget> targets) {
        return targets.stream()
                .sorted(Comparator.comparing(target -> target.getTargetType().name()))
                .map(this::toTargetResponse)
                .toList();
    }

    private AssignmentTargetResponse toTargetResponse(AssignmentTarget target) {
        User student = target.getStudentUser();
        com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = target.getClazz();
        return AssignmentTargetResponse.builder()
                .id(target.getId())
                .targetType(target.getTargetType())
                .classId(clazz == null ? null : clazz.getId())
                .className(clazz == null ? null : clazz.getName())
                .studentUserId(student == null ? null : student.getId())
                .studentFullName(student == null ? null : student.getFullName())
                .build();
    }

    private List<AssignmentRecipientResponse> toRecipientResponses(List<AssignmentRecipient> recipients) {
        return recipients.stream()
                .sorted(Comparator.comparing(recipient -> recipient.getStudentUser().getFullName()))
                .map(this::toRecipientResponse)
                .toList();
    }

    private AssignmentRecipientResponse toRecipientResponse(AssignmentRecipient recipient) {
        User student = recipient.getStudentUser();
        com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = recipient.getClazz();
        return AssignmentRecipientResponse.builder()
                .id(recipient.getId())
                .studentUserId(student.getId())
                .studentFullName(student.getFullName())
                .classId(clazz == null ? null : clazz.getId())
                .className(clazz == null ? null : clazz.getName())
                .sourceType(recipient.getSourceType())
                .status(recipient.getStatus())
                .assignedAt(recipient.getAssignedAt())
                .build();
    }

    private List<AssignmentItemResponse> toItemResponses(List<AssignmentItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(AssignmentItem::getDisplayOrder))
                .map(this::toItemResponse)
                .toList();
    }

    private AssignmentItemResponse toItemResponse(AssignmentItem item) {
        return AssignmentItemResponse.builder()
                .id(item.getId())
                .assessmentItemId(item.getAssessmentItem() == null ? null : item.getAssessmentItem().getId())
                .questionType(item.getQuestionType())
                .title(item.getTitle())
                .content(item.getContent())
                .difficulty(item.getDifficulty())
                .points(item.getPoints())
                .explanation(item.getExplanation())
                .sampleAnswer(item.getSampleAnswer())
                .gradingCriteriaName(item.getGradingCriteriaName())
                .gradingCriteriaContent(item.getGradingCriteriaContent())
                .displayOrder(item.getDisplayOrder())
                .options(toOptionResponses(item.getOptions()))
                .build();
    }

    private List<AssignmentItemOptionResponse> toOptionResponses(List<AssignmentItemOption> options) {
        return options.stream()
                .sorted(Comparator.comparing(AssignmentItemOption::getDisplayOrder))
                .map(option -> AssignmentItemOptionResponse.builder()
                        .id(option.getId())
                        .content(option.getContent())
                        .isCorrect(option.getIsCorrect())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }
}
