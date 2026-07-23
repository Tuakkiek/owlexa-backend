package com.owlexa.owlexabackend.modules.homework.dto.request.student;

import lombok.Data;
import java.util.List;

@Data
public class AutosaveQuestionSubmissionRequest {
    private Long questionId;
    private String textAnswer;
    private List<StudentHomeworkSubmissionAttachmentRequest> attachments;
    private List<Long> selectedOptionIds;
}
