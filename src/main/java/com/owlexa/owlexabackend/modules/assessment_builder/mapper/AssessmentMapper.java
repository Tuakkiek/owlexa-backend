package com.owlexa.owlexabackend.modules.assessment_builder.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentDetailResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentItemOptionResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentItemResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentListResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AssessmentMapper {

    private final RichTextDocumentService richTextDocumentService;
    private final FileMapper fileMapper;

    public AssessmentListResponse toListResponse(Assessment assessment) {
        return AssessmentListResponse.builder()
                .id(assessment.getId())
                .type(assessment.getType())
                .status(assessment.getStatus())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }

    public AssessmentDetailResponse toDetailResponse(Assessment assessment) {
        return AssessmentDetailResponse.builder()
                .id(assessment.getId())
                .type(assessment.getType())
                .status(assessment.getStatus())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .content(richTextDocumentService.deserialize(assessment.getContentJson()))
                .audioFileId(assessment.getAudioFile() == null ? null : assessment.getAudioFile().getId())
                .audioFile(toFileResponse(assessment.getAudioFile()))
                .playbackMode(assessment.getPlaybackMode())
                .items(toItemResponses(assessment.getItems()))
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }

    public AssessmentItem toItemSnapshot(Question question, BigDecimal points, Integer displayOrder) {
        GradingCriteria gradingCriteria = question.getGradingCriteria();
        AssessmentItem item = AssessmentItem.builder()
                .question(question)
                .questionType(question.getType())
                .title(null)
                .contentJson(question.getContentJson())
                .difficulty(question.getDifficulty())
                .points(points)
                .explanationJson(question.getExplanationJson())
                .sampleAnswerJson(question.getSampleAnswerJson())
                .gradingCriteria(gradingCriteria)
                .gradingCriteriaName(gradingCriteria == null ? null : gradingCriteria.getName())
                .gradingCriteriaContentJson(gradingCriteria == null ? null : gradingCriteria.getContentJson())
                .displayOrder(displayOrder)
                .build();

        question.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getDisplayOrder))
                .map(this::toOptionSnapshot)
                .forEach(option -> {
                    option.setAssessmentItem(item);
                    item.getOptions().add(option);
                });

        return item;
    }

    private AssessmentItemOption toOptionSnapshot(QuestionOption option) {
        return AssessmentItemOption.builder()
                .content(option.getContent())
                .isCorrect(option.getIsCorrect())
                .displayOrder(option.getDisplayOrder())
                .build();
    }

    private List<AssessmentItemResponse> toItemResponses(List<AssessmentItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(AssessmentItem::getDisplayOrder))
                .map(this::toItemResponse)
                .toList();
    }

    private AssessmentItemResponse toItemResponse(AssessmentItem item) {
        return AssessmentItemResponse.builder()
                .id(item.getId())
                .questionId(item.getQuestion().getId())
                .questionType(item.getQuestionType())
                .title(item.getTitle())
                .content(richTextDocumentService.deserialize(item.getContentJson()))
                .difficulty(item.getDifficulty())
                .points(item.getPoints())
                .explanation(richTextDocumentService.deserializeOptional(item.getExplanationJson()))
                .sampleAnswer(richTextDocumentService.deserializeOptional(item.getSampleAnswerJson()))
                .gradingCriteriaId(item.getGradingCriteria() == null ? null : item.getGradingCriteria().getId())
                .gradingCriteriaName(item.getGradingCriteriaName())
                .gradingCriteriaContent(richTextDocumentService.deserializeOptional(item.getGradingCriteriaContentJson()))
                .displayOrder(item.getDisplayOrder())
                .options(toOptionResponses(item.getOptions()))
                .build();
    }

    private List<AssessmentItemOptionResponse> toOptionResponses(List<AssessmentItemOption> options) {
        return options.stream()
                .sorted(Comparator.comparing(AssessmentItemOption::getDisplayOrder))
                .map(option -> AssessmentItemOptionResponse.builder()
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
