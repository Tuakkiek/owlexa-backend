package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.EssaySubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EssaySubmissionResponse {
    private Long id;
    private Long studentId;
    private String studentFullName;
    private Long classId;
    private String className;
    private Long rubricId;
    private String rubricTitle;
    private String content;
    private EssaySubmissionStatus status;
    private Instant submittedAt;
    private Instant createdAt;
}
