package com.owlexa.owlexabackend.modules.class_management.dto.response;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
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
    private Integer maxStudents;
    private Double monthFee;
    private ClassStatus status;
    private Boolean isActive;
    private Long centerId;

    private Long courseId;
    private String courseName;
    private String courseCode;

}
