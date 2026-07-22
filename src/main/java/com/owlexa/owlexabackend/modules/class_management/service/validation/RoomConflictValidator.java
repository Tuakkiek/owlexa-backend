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
                    String.format("Room %s is already occupied on %s from %s to %s.",
                            context.getRoom().getName(), dayName, startStr, endStr)
            );
        }
    }

    private String formatDayOfWeek(java.time.DayOfWeek day) {
        String name = day.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
