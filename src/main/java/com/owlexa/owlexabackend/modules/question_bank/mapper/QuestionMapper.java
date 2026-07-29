package com.owlexa.owlexabackend.modules.question_bank.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.GradingCriteriaSummaryResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionOptionResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionMapper {

    private final RichTextDocumentService richTextDocumentService;
    private final QuestionCollectionMapper collectionMapper;

    public QuestionResponse toListResponse(Question question) {
        return toResponse(question, null);
    }

    public QuestionResponse toDetailResponse(Question question) {
        return toResponse(question, toOptionResponses(question));
    }

    private QuestionResponse toResponse(
            Question question,
            List<QuestionOptionResponse> options
    ) {
        GradingCriteria criteria = question.getGradingCriteria();

        return QuestionResponse.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .collection(collectionMapper.toSummary(question.getCollection()))
                .sectionCode(question.getSectionCode())
                .displayOrder(question.getDisplayOrder())
                .type(question.getType())
                .content(richTextDocumentService.deserialize(question.getContentJson()))
                .difficulty(question.getDifficulty())
                .points(question.getPoints())
                .gradingCriteria(criteria == null ? null : toGradingCriteriaSummary(criteria))
                .explanation(richTextDocumentService.deserializeOptional(question.getExplanationJson()))
                .sampleAnswer(richTextDocumentService.deserializeOptional(question.getSampleAnswerJson()))
                .options(options)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private GradingCriteriaSummaryResponse toGradingCriteriaSummary(GradingCriteria criteria) {
        return GradingCriteriaSummaryResponse.builder()
                .id(criteria.getId())
                .name(criteria.getName())
                .build();
    }

    private List<QuestionOptionResponse> toOptionResponses(Question question) {
        return question.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getDisplayOrder))
                .map(option -> QuestionOptionResponse.builder()
                        .id(option.getId())
                        .content(option.getContent())
                        .isCorrect(option.getIsCorrect())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }
}
