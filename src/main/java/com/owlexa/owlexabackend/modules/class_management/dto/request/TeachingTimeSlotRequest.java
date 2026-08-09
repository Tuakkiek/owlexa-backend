package com.owlexa.owlexabackend.modules.class_management.dto.request;

import com.owlexa.owlexabackend.modules.class_management.entity.TimeSlotPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingTimeSlotRequest {

    @NotBlank(message = "Tên ca học không được để trống")
    private String name;

    @NotNull(message = "Vui lòng chọn buổi")
    private TimeSlotPeriod period;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private Integer displayOrder;

    private Boolean isActive;
}
