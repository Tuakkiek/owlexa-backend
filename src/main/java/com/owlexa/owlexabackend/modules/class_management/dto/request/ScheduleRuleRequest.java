package com.owlexa.owlexabackend.modules.class_management.dto.request;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ScheduleRuleRequest {
    @NotNull(message = "Please select a teacher")
    private Long teacherUserId;

    @NotNull(message = "Please select a room")
    private Long roomId;

    @NotEmpty(message = "Please select at least one weekday")
    private List<Integer> daysOfWeek;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private ScheduleType type;
}
