package com.owlexa.owlexabackend.modules.question_bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCollectionResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private long questionCount;
    private Instant createdAt;
    private Instant updatedAt;
}
