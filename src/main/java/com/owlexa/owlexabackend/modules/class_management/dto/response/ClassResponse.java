package com.owlexa.owlexabackend.modules.class_management.dto.response;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.Instant;

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

    private Long studentCount;
    private Long scheduleCount;
    private List<String> teachers;
    private Instant createdAt;
}
