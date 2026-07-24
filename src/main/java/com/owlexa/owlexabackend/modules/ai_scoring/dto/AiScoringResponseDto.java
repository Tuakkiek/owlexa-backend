package com.owlexa.owlexabackend.modules.ai_scoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiScoringResponseDto {
    private AiBulkScoringResult result;
    private Integer promptTokens;
    private Integer responseTokens;
    private Integer totalTokens;
    private Long latencyMs;
    private String modelUsed;
}
