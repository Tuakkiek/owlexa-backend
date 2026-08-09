package com.owlexa.owlexabackend.modules.student_submission.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioProgressUpdateRequest {

    @Min(0)
    private Integer positionSeconds;
    
    private Boolean completed;
}
