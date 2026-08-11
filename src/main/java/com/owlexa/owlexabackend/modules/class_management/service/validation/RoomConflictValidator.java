package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomConflictValidator implements ScheduleValidator {

    private final ScheduleRepository scheduleRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void validate(ScheduleValidationContext context) {
        if (context.getRoom() == null) {
            return;
        }

        List<Schedule> overlaps = scheduleRepository.findOverlappingRoomSchedules(
                context.getRoom().getId(),
                context.getDayOfWeek(),
                context.getStartTime(),
                context.getEndTime(),
                context.getCenterId(),
                context.getScheduleId()
        );

        if (!overlaps.isEmpty()) {
            Schedule conflict = overlaps.get(0);
            String dayName = formatDayOfWeek(conflict.getDayOfWeek());
            String startStr = conflict.getStartTime().format(TIME_FORMATTER);
            String endStr = conflict.getEndTime().format(TIME_FORMATTER);
            throw new BusinessRuleException(
                    "ROOM_CONFLICT",
                    String.format("Phòng %s đã có lịch vào %s từ %s đến %s.",
                            context.getRoom().getName(), dayName, startStr, endStr)
            );
        }
    }

    private String formatDayOfWeek(java.time.DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}
