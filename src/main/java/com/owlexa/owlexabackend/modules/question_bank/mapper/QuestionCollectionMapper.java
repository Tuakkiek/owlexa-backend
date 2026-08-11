package com.owlexa.owlexabackend.modules.question_bank.mapper;

import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionSummaryResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import org.springframework.stereotype.Component;

@Component
public class QuestionCollectionMapper {

    public QuestionCollectionResponse toResponse(QuestionCollection collection, long questionCount) {
        return QuestionCollectionResponse.builder()
                .id(collection.getId())
                .code(collection.getCode())
                .name(collection.getName())
                .description(collection.getDescription())
                .questionCount(questionCount)
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }

    public QuestionCollectionSummaryResponse toSummary(QuestionCollection collection) {
        return QuestionCollectionSummaryResponse.builder()
                .id(collection.getId())
                .code(collection.getCode())
                .name(collection.getName())
                .build();
    }
}
