package com.owlexa.owlexabackend.modules.class_management.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    @NotNull(message = "teacherUserId is required")
    private Long teacherUserId;

    @NotNull(message = "roomId is required")
    private Long roomId;

    @NotNull(message = "dayOfWeek is required")
    @Min(value = 0, message = "dayOfWeek must be between 0 and 6")
    @Max(value = 6, message = "dayOfWeek must be between 0 and 6")
    private Integer dayOfWeek;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    private ScheduleType type;
}
