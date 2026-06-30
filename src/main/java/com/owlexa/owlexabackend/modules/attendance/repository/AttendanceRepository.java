package com.owlexa.owlexabackend.modules.attendance.repository;

import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findAllByScheduleIdAndDate(Long scheduleId, LocalDate date);

    Optional<Attendance> findByScheduleIdAndStudentUserIdAndDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate date
    );

    boolean existsByScheduleIdAndStudentUserIdAndDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate date
    );

    long countByScheduleIdAndDateAndStatus(
            Long scheduleId,
            LocalDate date,
            AttendanceStatus status
    );

    void deleteByCenterId(Long centerId);
}
