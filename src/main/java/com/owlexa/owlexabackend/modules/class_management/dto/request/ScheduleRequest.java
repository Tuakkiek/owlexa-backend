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

    @NotNull(message = "Vui lòng chọn giáo viên")
    private Long teacherUserId;

    @NotNull(message = "Vui lòng chọn phòng học")
    private Long roomId;

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Min(value = 0, message = "Thứ trong tuần phải từ 0 đến 6 (0 là Chủ Nhật)")
    @Max(value = 6, message = "Thứ trong tuần phải từ 0 đến 6 (0 là Chủ Nhật)")
    private Integer dayOfWeek;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private ScheduleType type;
}
