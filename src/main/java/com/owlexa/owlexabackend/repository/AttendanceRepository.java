package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Attendance;
import com.owlexa.owlexabackend.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findAllByScheduleIdAndSessionDate(Long scheduleId, LocalDate sessionDate);

    Optional<Attendance> findByScheduleIdAndStudentUserIdAndSessionDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate sessionDate
    );

    boolean existsByScheduleIdAndStudentUserIdAndSessionDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate sessionDate
    );

    long countByScheduleIdAndSessionDateAndStatus(
            Long scheduleId,
            LocalDate sessionDate,
            AttendanceStatus status
    );
}