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
public class QuestionImportResultResponse {

    private int importedCount;
    private List<QuestionResponse> questions;
}
