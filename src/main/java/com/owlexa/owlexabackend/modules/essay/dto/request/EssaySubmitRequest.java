package com.owlexa.owlexabackend.modules.essay.dto.request;
import lombok.Data;

@Data
public class EssaySubmitRequest {
    private Long rubricId;
    private String content;
}
