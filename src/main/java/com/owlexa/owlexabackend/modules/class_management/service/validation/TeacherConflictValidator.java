package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TeacherConflictValidator implements ScheduleValidator {

    private final ScheduleRepository scheduleRepository;

    @Override
    public void validate(ScheduleValidationContext context) {
        if (context.getTeacher() == null) {
            return;
        }

        List<Schedule> overlaps = scheduleRepository.findOverlappingTeacherSchedules(
                context.getTeacher().getId(),
                context.getDayOfWeek(),
                context.getStartTime(),
                context.getEndTime(),
                context.getCenterId(),
                context.getScheduleId()
        );

        if (!overlaps.isEmpty()) {
            throw new BusinessRuleException(
                    "TEACHER_CONFLICT",
                    String.format("Giáo viên %s đã có lớp khác vào thời gian này.",
                            context.getTeacher().getFullName())
            );
        }
    }
}
