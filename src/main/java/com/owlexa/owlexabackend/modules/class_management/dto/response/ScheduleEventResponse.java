package com.owlexa.owlexabackend.modules.class_management.dto.response;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEventResponse {
    private Long id;
    private Long classId;
    private String className;
    private Long recurringRuleId;
    private Long teacherUserId;
    private String teacherUserFullName;
    private Long roomId;
    private String roomName;
    private LocalDate eventDate;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer lessonNumber;
    private ScheduleEventType eventType;
    private ScheduleEventStatus status;
    private String title;
    private String note;
}
