package com.owlexa.owlexabackend.modules.attendance.repository;

import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findAllBySchedule_IdAndDate(Long scheduleId, LocalDate date);

    Optional<Attendance> findBySchedule_IdAndStudentUser_IdAndDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate date
    );

    boolean existsBySchedule_IdAndStudentUser_IdAndDate(
            Long scheduleId,
            Long studentUserId,
            LocalDate date
    );

    long countBySchedule_IdAndDateAndStatus(
            Long scheduleId,
            LocalDate date,
            AttendanceStatus status
    );

    void deleteByCenter_Id(Long centerId);
}
