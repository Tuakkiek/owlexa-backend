package com.owlexa.owlexabackend.modules.class_management.repository;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    // Find all by classId and centerId
    List<Schedule> findAllByClazzIdAndCenterId(Long classId, Long centerId);

    // Find all by teacherUserId and centerId
    List<Schedule> findAllByTeacherUserIdAndCenterId(Long teacherUserId, Long centerId);

    // Exists by clazzId and dayOfWeek and start time and centerId
    boolean existsByClazzIdAndDayOfWeekAndStartTimeAndCenterId(
            Long classId,
            Integer dayOfWeek,
            java.time.LocalTime startTime,
            Long centerId
    );

    void deleteByCenterId(Long centerId);
}
