package com.owlexa.owlexabackend.modules.essay.dto.response;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class EssayRubricResponse {
    private Long id;
    private Long classId;
    private String className;
    private String title;
    private String description;
    private Double maxScore;
    private List<CriterionResponse> criteria;
    private Instant createdAt;
    private Boolean isActive;

    @Data
    @Builder
    public static class CriterionResponse {
        private Long id;
        private String name;
        private String description;
        private Double weight;
        private Double maxScore;
    }
}
