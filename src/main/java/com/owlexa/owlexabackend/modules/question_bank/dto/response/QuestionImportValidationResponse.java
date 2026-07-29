package com.owlexa.owlexabackend.modules.question_bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionImportValidationResponse {

    private String version;
    private Long collectionId;
    private String collectionName;
    private String collectionCode;
    private int questionCount;
    private List<QuestionImportPreviewItemResponse> questions;
}
