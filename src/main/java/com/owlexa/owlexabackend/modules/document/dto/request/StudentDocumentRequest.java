package com.owlexa.owlexabackend.modules.document.dto.request;
import com.owlexa.owlexabackend.modules.document.entity.DocumentType;
import lombok.Data;

@Data
public class StudentDocumentRequest {
    private String title;
    private DocumentType type;
    private String url;
    private String description;
}
