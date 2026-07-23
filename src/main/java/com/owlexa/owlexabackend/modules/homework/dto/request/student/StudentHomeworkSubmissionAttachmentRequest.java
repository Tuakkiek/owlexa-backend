package com.owlexa.owlexabackend.modules.homework.dto.request.student;

import lombok.Data;

@Data
public class StudentHomeworkSubmissionAttachmentRequest {
    private String fileUrl;
    private String fileName;
    private String fileType;
}
