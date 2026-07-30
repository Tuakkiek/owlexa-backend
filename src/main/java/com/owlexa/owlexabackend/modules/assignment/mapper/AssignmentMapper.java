package com.owlexa.owlexabackend.modules.assignment.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentItemOptionResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentItemResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentRecipientResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentTargetResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItemOption;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTarget;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AssignmentMapper {

    private final RichTextDocumentService richTextDocumentService;
    private final FileMapper fileMapper;

    public AssignmentListResponse toListResponse(Assignment assignment) {
        return AssignmentListResponse.builder()
                .id(assignment.getId())
                .assessmentId(assignment.getAssessment().getId())
                .type(assignment.getType())
                .status(assignment.getStatus())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .content(richTextDocumentService.deserialize(assignment.getContentJson()))
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
                .content(richTextDocumentService.deserialize(assignment.getContentJson()))
                .openAt(assignment.getOpenAt())
                .dueAt(assignment.getDueAt())
                .attemptLimit(assignment.getAttemptLimit())
                .assessmentSnapshotAt(assignment.getAssessmentSnapshotAt())
                .audioFile(toFileResponse(assignment.getAudioFile()))
                .playbackMode(assignment.getPlaybackMode())
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

    public AssignmentItem toItemSnapshot(AssessmentItem assessmentItem) {
        AssignmentItem item = AssignmentItem.builder()
                .assessmentItem(assessmentItem)
                .questionType(assessmentItem.getQuestionType())
                .title(assessmentItem.getTitle())
                .contentJson(assessmentItem.getContentJson())
                .difficulty(assessmentItem.getDifficulty())
                .points(assessmentItem.getPoints())
                .explanationJson(assessmentItem.getExplanationJson())
                .sampleAnswerJson(assessmentItem.getSampleAnswerJson())
                .gradingCriteriaName(assessmentItem.getGradingCriteriaName())
                .gradingCriteriaContentJson(assessmentItem.getGradingCriteriaContentJson())
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

    private FileResponse toFileResponse(StoredFile file) {
        return file == null ? null : fileMapper.toResponse(file);
    }
}
