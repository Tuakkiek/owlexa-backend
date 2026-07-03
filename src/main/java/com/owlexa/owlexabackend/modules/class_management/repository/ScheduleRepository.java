package com.owlexa.owlexabackend.modules.class_management.repository;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByClazz_IdAndCenter_Id(Long classId, Long centerId);

    List<Schedule> findAllByTeacherUser_IdAndCenter_Id(Long teacherUserId, Long centerId);

    List<Schedule> findAllByCenter_Id(Long centerId);

    boolean existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
            Long classId,
            Integer dayOfWeek,
            java.time.LocalTime startTime,
            Long centerId
    );

    void deleteByCenter_Id(Long centerId);
}
