package com.owlexa.owlexabackend.modules.class_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class QuickSetupRequest {

    @NotNull(message = "Thời lượng ca học không được để trống")
    @Min(value = 15, message = "Thời lượng ca học tối thiểu 15 phút")
    @Max(value = 300, message = "Thời lượng ca học tối đa 300 phút")
    private Integer durationMinutes;

    @NotNull(message = "Thời gian nghỉ không được để trống")
    @Min(value = 0, message = "Thời gian nghỉ tối thiểu 0 phút")
    @Max(value = 120, message = "Thời gian nghỉ tối đa 120 phút")
    private Integer gapMinutes;

    private LocalTime morningStart;

    @Min(value = 0, message = "Số ca sáng không hợp lệ")
    private Integer morningCount;

    private LocalTime afternoonStart;

    @Min(value = 0, message = "Số ca chiều không hợp lệ")
    private Integer afternoonCount;

    private LocalTime eveningStart;

    @Min(value = 0, message = "Số ca tối không hợp lệ")
    private Integer eveningCount;
}
