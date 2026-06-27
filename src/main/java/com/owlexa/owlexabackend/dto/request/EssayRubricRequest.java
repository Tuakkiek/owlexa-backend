package com.owlexa.owlexabackend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class EssayRubricRequest {
    private Long classId;
    private String title;
    private String description;
    private Double maxScore;
    private List<CriterionRequest> criteria;

    @Data
    public static class CriterionRequest {
        private String name;
        private String description;
        private Double weight;
        private Double maxScore;
    }
}
