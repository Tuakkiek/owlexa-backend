package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportRequest {

    @NotNull(message = "Collection id is required")
    private Long collectionId;

    @NotBlank(message = "Import JSON is required")
    private String json;
}
