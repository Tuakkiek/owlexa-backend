package com.owlexa.owlexabackend.dto.request;

import lombok.Data;

@Data
public class EssaySubmitRequest {
    private Long rubricId;
    private String content;
}
