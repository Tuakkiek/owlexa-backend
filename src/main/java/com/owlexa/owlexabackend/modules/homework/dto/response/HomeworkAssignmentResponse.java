package com.owlexa.owlexabackend.modules.homework.dto.response;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class HomeworkAssignmentResponse {
    private Long id;
    
    // Extracted Template Info
    private Long templateId;
    private String templateTitle;
    private Double maxScore;
    
    // Class Info
    private Long clazzId;
    private String clazzName;
    
    // Teacher Info
    private Long teacherId;
    private String teacherFullName;

    // Timeline & Status
    private HomeworkAssignmentStatus status;
    private Instant availableFrom;
    private Instant dueDate;
    private Instant closeAt;
    
    private Instant scheduledAt;
    private Instant openedAt;
    private Instant closedAt;
    private Instant archivedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Settings
    private Boolean allowLateSubmission;
    private Boolean allowResubmit;
    private Boolean publishScoreImmediately;
    private Boolean isGradesReleased;
    private Boolean showAnswerAfterGrading;

    // Computed Progress Stats
    private Long totalStudents;
    private Long submittedCount;
    private Long gradedCount;
    private Long missingCount;
    private Long lateCount;
}
