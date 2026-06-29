package com.owlexa.owlexabackend.modules.essay.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EssayDetailResponse {
    private EssaySubmissionResponse essay;
    private EssayGradingResultResponse gradingResult;
}
