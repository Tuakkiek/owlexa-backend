package com.owlexa.owlexabackend.modules.class_management.service.validation;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleValidatorsTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @InjectMocks
    private RoomConflictValidator roomConflictValidator;

    @InjectMocks
    private TeacherConflictValidator teacherConflictValidator;

    @InjectMocks
    private StudentConflictValidator studentConflictValidator;

    @Test
    void classLifecycleValidator_whenFinished_shouldThrow() {
        ClassLifecycleValidator validator = new ClassLifecycleValidator();
        Class clazz = new Class();
        clazz.setStatus(ClassStatus.FINISHED);

        ScheduleValidationContext context = ScheduleValidationContext.builder()
                .clazz(clazz)
                .build();

        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("trạng thái dự kiến hoặc đang học")
                .extracting("code")
                .isEqualTo("CLASS_FINISHED");
    }

    @Test
    void timeRangeValidator_whenStartNotBeforeEnd_shouldThrow() {
        TimeRangeValidator validator = new TimeRangeValidator();
        ScheduleValidationContext context = ScheduleValidationContext.builder()
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giờ bắt đầu phải trước giờ kết thúc")
                .extracting("code")
                .isEqualTo("INVALID_TIME_RANGE");
    }

    @Test
    void roomConflictValidator_whenOverlaps_shouldThrow() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Room 101");

        ScheduleValidationContext context = ScheduleValidationContext.builder()
                .room(room)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .centerId(1L)
                .build();

        Schedule conflict = new Schedule();
        conflict.setRoom(room);
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(9, 0));
        conflict.setEndTime(LocalTime.of(11, 0));

        when(scheduleRepository.findOverlappingRoomSchedules(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> roomConflictValidator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Phòng Room 101 đã có lịch vào Thứ Hai từ 09:00 đến 11:00.")
                .extracting("code")
                .isEqualTo("ROOM_CONFLICT");
    }

    @Test
    void teacherConflictValidator_whenOverlaps_shouldThrow() {
        User teacher = new User();
        teacher.setId(2L);
        teacher.setFullName("David Nguyen");

        ScheduleValidationContext context = ScheduleValidationContext.builder()
                .teacher(teacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .centerId(1L)
                .build();

        Schedule conflict = new Schedule();
        conflict.setTeacherUser(teacher);
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(9, 0));
        conflict.setEndTime(LocalTime.of(11, 0));

        when(scheduleRepository.findOverlappingTeacherSchedules(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> teacherConflictValidator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Giáo viên David Nguyen đã có lớp khác vào thời gian này.")
                .extracting("code")
                .isEqualTo("TEACHER_CONFLICT");
    }

    @Test
    void studentConflictValidator_whenStudentOverlaps_shouldThrow() {
        Class clazz = new Class();
        clazz.setId(5L);

        User student = new User();
        student.setId(10L);
        student.setFullName("John Smith");

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setStudentUser(student);

        ScheduleValidationContext context = ScheduleValidationContext.builder()
                .clazz(clazz)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .centerId(1L)
                .build();

        when(classEnrollmentRepository.findAllByClazz_IdAndStatusIn(any(), any()))
                .thenReturn(List.of(enrollment));

        Schedule conflict = new Schedule();
        conflict.setDayOfWeek(DayOfWeek.MONDAY);
        conflict.setStartTime(LocalTime.of(9, 0));
        conflict.setEndTime(LocalTime.of(11, 0));

        when(scheduleRepository.findOverlappingStudentSchedules(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> studentConflictValidator.validate(context))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Học viên John Smith đã có lớp khác vào thời gian này.")
                .extracting("code")
                .isEqualTo("STUDENT_CONFLICT");
    }
}
