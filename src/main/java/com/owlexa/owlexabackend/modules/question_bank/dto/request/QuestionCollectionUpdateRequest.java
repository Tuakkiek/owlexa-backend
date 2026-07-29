package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCollectionUpdateRequest {

    @NotBlank(message = "Collection name is required")
    @Size(max = 255, message = "Collection name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Collection description must not exceed 1000 characters")
    private String description;
}
