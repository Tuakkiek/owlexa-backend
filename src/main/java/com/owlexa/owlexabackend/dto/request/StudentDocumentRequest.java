package com.owlexa.owlexabackend.dto.request;

import com.owlexa.owlexabackend.entity.DocumentType;
import lombok.Data;

@Data
public class StudentDocumentRequest {
    private String title;
    private DocumentType type;
    private String url;
}
