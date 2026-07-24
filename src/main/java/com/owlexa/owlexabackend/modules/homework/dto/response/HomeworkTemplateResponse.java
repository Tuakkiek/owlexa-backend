package com.owlexa.owlexabackend.modules.homework.dto.response;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestion;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class HomeworkTemplateResponse {
    private Long id;
    private String title;
    private String description;
    private String instructions;
    private HomeworkType homeworkType;
    private Integer estimatedTime;
    private Boolean archived;
    private Integer version;
    private Long parentTemplateId;
    private Double maxScore;
    
    private Long teacherId;
    private String teacherFullName;

    private List<HomeworkQuestion> questions;

    private Instant createdAt;
    private Instant updatedAt;
    
    // UI Projections
    private Long assignmentCount;
    private Long activeAssignmentCount;
    private String status; // "DRAFT", "ACTIVE", "ARCHIVED"
}
