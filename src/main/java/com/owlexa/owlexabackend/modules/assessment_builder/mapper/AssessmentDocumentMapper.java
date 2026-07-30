package com.owlexa.owlexabackend.modules.assessment_builder.mapper;

import com.owlexa.owlexabackend.common.assessment_document.AssessmentBlockType;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentBlockResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentDocumentResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentQuestionPreviewResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentBlock;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AssessmentDocumentMapper {

    private final ObjectMapper objectMapper;

    public AssessmentDocumentResponse toDocumentResponse(
            Assessment assessment,
            List<AssessmentBlockResponse> blocks
    ) {
        List<AssessmentBlockResponse> orderedBlocks = blocks == null
                ? List.of()
                : blocks.stream()
                .sorted(Comparator.comparing(AssessmentBlockResponse::getPosition))
                .toList();

        return AssessmentDocumentResponse.builder()
                .id(assessment.getId())
                .title(assessment.getTitle())
                .type(assessment.getType())
                .status(assessment.getStatus())
                .documentFormat(assessment.getDocumentFormat())
                .version(assessment.getVersion())
                .blocks(orderedBlocks)
                .totalQuestions(countQuestionBlocks(orderedBlocks))
                .totalPoints(sumQuestionPoints(orderedBlocks))
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }

    public AssessmentBlockResponse toBlockResponse(
            AssessmentBlock block,
            FileResponse file,
            AssessmentQuestionPreviewResponse questionPreview
    ) {
        return AssessmentBlockResponse.builder()
                .id(block.getId())
                .blockType(block.getBlockType())
                .position(block.getPosition())
                .content(readOptionalJson(block.getContentJson()))
                .file(file)
                .caption(block.getCaption())
                .alignment(block.getAlignment())
                .questionId(block.getQuestion() == null ? null : block.getQuestion().getId())
                .points(block.getPoints())
                .questionPreview(questionPreview)
                .build();
    }

    public AssessmentQuestionPreviewResponse toQuestionPreview(Question question) {
        if (question == null) {
            return null;
        }
        return AssessmentQuestionPreviewResponse.builder()
                .questionCode(question.getQuestionCode())
                .questionType(question.getType())
                .content(readJson(question.getContentJson()))
                .difficulty(question.getDifficulty())
                .options(toQuestionOptionPreviews(question.getOptions()))
                .build();
    }

    public AssessmentQuestionPreviewResponse toQuestionPreview(AssessmentItem snapshotItem) {
        if (snapshotItem == null) {
            return null;
        }
        return AssessmentQuestionPreviewResponse.builder()
                .questionCode(null)
                .questionType(snapshotItem.getQuestionType())
                .content(readJson(snapshotItem.getContentJson()))
                .difficulty(snapshotItem.getDifficulty())
                .options(toAssessmentItemOptionPreviews(snapshotItem.getOptions()))
                .build();
    }

    private long countQuestionBlocks(List<AssessmentBlockResponse> blocks) {
        return blocks.stream()
                .filter(block -> block.getBlockType() == AssessmentBlockType.QUESTION)
                .count();
    }

    private BigDecimal sumQuestionPoints(List<AssessmentBlockResponse> blocks) {
        return blocks.stream()
                .filter(block -> block.getBlockType() == AssessmentBlockType.QUESTION)
                .map(block -> {
                    if (block.getPoints() == null) {
                        throw new IllegalStateException("Question block points must not be null");
                    }
                    return block.getPoints();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AssessmentQuestionPreviewResponse.OptionPreview> toQuestionOptionPreviews(
            List<QuestionOption> options
    ) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .sorted(Comparator.comparing(QuestionOption::getDisplayOrder))
                .map(option -> AssessmentQuestionPreviewResponse.OptionPreview.builder()
                        .content(option.getContent())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }

    private List<AssessmentQuestionPreviewResponse.OptionPreview> toAssessmentItemOptionPreviews(
            List<AssessmentItemOption> options
    ) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .sorted(Comparator.comparing(AssessmentItemOption::getDisplayOrder))
                .map(option -> AssessmentQuestionPreviewResponse.OptionPreview.builder()
                        .content(option.getContent())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }

    private JsonNode readOptionalJson(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return null;
        }
        return readJson(serialized);
    }

    private JsonNode readJson(String serialized) {
        try {
            return objectMapper.readTree(serialized);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Stored assessment document content is invalid", exception);
        }
    }
}
