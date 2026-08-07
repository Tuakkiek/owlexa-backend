package com.owlexa.owlexabackend.modules.class_management.dto.response;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRepeatType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRuleResponse {
    private Long id;
    private Long classId;
    private Long teacherUserId;
    private String teacherUserFullName;
    private Long roomId;
    private String roomName;
    private ScheduleRepeatType repeatType;
    private List<Integer> daysOfWeek;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleType type;
    private Boolean isActive;
    private Long generatedEventCount;
}
