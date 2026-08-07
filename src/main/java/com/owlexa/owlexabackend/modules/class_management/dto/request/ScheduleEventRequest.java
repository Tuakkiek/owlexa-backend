package com.owlexa.owlexabackend.modules.class_management.dto.request;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import jakarta.validation.constraints.NotNull;
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
public class ScheduleEventRequest {
    private Long teacherUserId;
    private Long roomId;

    @NotNull(message = "Ngày diễn ra không được để trống")
    private LocalDate eventDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    @NotNull(message = "Loại sự kiện không được để trống")
    private ScheduleEventType eventType;

    private ScheduleEventStatus status;
    private String title;
    private String note;
}
