package com.owlexa.owlexabackend.modules.question_bank.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingCriteriaSummaryResponse {

    private Long id;
    private String name;
}
