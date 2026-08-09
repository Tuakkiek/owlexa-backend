package com.owlexa.owlexabackend.modules.class_management.dto.request;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRuleRequest {
    @NotNull(message = "Vui lòng chọn giáo viên")
    private Long teacherUserId;

    @NotNull(message = "Vui lòng chọn phòng học")
    private Long roomId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một thứ trong tuần")
    private List<Integer> daysOfWeek;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Vui lòng chọn ca học")
    private Long timeSlotId;

    private ScheduleType type;
}
