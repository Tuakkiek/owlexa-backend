package com.owlexa.owlexabackend.modules.student_submission.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartAttemptRequest {
    private String password;
}
