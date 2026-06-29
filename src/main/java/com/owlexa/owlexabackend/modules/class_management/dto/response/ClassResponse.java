package com.owlexa.owlexabackend.modules.class_management.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassResponse {

    private Long id;
    private String name;
    private String vstepLevel;
    private Integer maxStudents;
    private Double monthFee;
    private Boolean isActive;
    private Long centerId;
}
