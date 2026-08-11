package com.owlexa.owlexabackend.modules.teacher_attendance.repository;

import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

    List<TeacherAttendance> findAllByTeacherUser_IdAndDate(Long teacherUserId, LocalDate date);

    List<TeacherAttendance> findAllByTeacherUser_IdAndDateBetween(
            Long teacherUserId, LocalDate startDate, LocalDate endDate);

    List<TeacherAttendance> findAllByCenter_IdAndDate(Long centerId, LocalDate date);

    List<TeacherAttendance> findAllByCenter_IdAndDateBetween(
            Long centerId, LocalDate startDate, LocalDate endDate);

    Optional<TeacherAttendance> findByScheduleEvent_IdAndTeacherUser_Id(Long scheduleEventId, Long teacherUserId);

    Optional<TeacherAttendance> findByScheduleEvent_Id(Long scheduleEventId);

    List<TeacherAttendance> findAllByCenter_IdAndScheduleEvent_IdIn(Long centerId, List<Long> scheduleEventIds);

    Optional<TeacherAttendance> findByTeacherUser_IdAndDate(Long teacherUserId, LocalDate date);

    boolean existsByTeacherUser_IdAndDate(Long teacherUserId, LocalDate date);

    void deleteByCenter_Id(Long centerId);
}
