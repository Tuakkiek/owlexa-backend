package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudentDocumentResponse {
    private Long id;
    private String title;
    private DocumentType type;
    private Instant uploadedAt;
    private String url;
    private Long classId;
    private String className;
}
