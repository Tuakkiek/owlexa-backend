package com.owlexa.owlexabackend.modules.question_bank.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBulkDeleteRequest {

    @NotEmpty(message = "Question ids are required")
    private List<Long> questionIds;
}
