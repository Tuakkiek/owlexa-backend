package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import lombok.Data;

@Data
public class StudentHomeworkSubmissionAttachmentResponse {
    private Long id;
    private String fileUrl;
    private String fileName;
    private String fileType;
}
