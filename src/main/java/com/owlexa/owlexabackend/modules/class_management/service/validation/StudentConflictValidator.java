package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentConflictValidator implements ScheduleValidator {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public void validate(ScheduleValidationContext context) {
        if (context.getClazz() == null) {
            return;
        }

        // Find all students currently enrolled in the class (ACTIVE, PENDING, SUSPENDED)
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findAllByClazz_IdAndStatusIn(
                context.getClazz().getId(),
                List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING, EnrollmentStatus.SUSPENDED)
        );

        for (ClassEnrollment enrollment : enrollments) {
            List<Schedule> overlaps = scheduleRepository.findOverlappingStudentSchedules(
                    enrollment.getStudentUser().getId(),
                    context.getDayOfWeek(),
                    context.getStartTime(),
                    context.getEndTime(),
                    context.getCenterId(),
                    context.getScheduleId()
            );

            if (!overlaps.isEmpty()) {
                throw new BusinessRuleException(
                        "STUDENT_CONFLICT",
                        String.format("Student %s already has another class during this time.",
                                enrollment.getStudentUser().getFullName())
                );
            }
        }
    }
}
