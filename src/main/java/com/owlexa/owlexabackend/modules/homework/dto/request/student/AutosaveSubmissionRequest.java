package com.owlexa.owlexabackend.modules.homework.dto.request.student;

import lombok.Data;
import java.util.List;

@Data
public class AutosaveSubmissionRequest {
    private Long version;
    private List<AutosaveQuestionSubmissionRequest> questionSubmissions;
}
