package com.owlexa.owlexabackend.modules.attendance.repository;

import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Student self-view: attendance for a specific student in a class on a date */
    List<Attendance> findByStudentUser_IdAndSchedule_Clazz_IdAndDate(
            Long studentUserId,
            Long classId,
            LocalDate date
    );

    /** Student self-view: attendance for a specific student in a class across a date range */
    List<Attendance> findByStudentUser_IdAndSchedule_Clazz_IdAndDateBetween(
            Long studentUserId,
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );

    /** Student self-view: all attendance for a student (across all classes) */
    List<Attendance> findByStudentUser_IdAndDate(Long studentUserId, LocalDate date);

    /** Owner statistics: count attendance by status for a class in a date range */
    @Query("SELECT a.status, COUNT(a) FROM Attendance a " +
           "WHERE a.schedule.clazz.id = :classId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "AND a.center.id = :centerId " +
           "GROUP BY a.status")
    List<Object[]> countByClassAndDateRangeGroupByStatus(
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("centerId") Long centerId
    );

    /** Owner view: attendance for a class on a specific date */
    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.studentUser " +
           "WHERE a.schedule.clazz.id = :classId " +
           "AND a.date = :date " +
           "AND a.center.id = :centerId")
    List<Attendance> findAllByClassIdAndDate(
            @Param("classId") Long classId,
            @Param("date") LocalDate date,
            @Param("centerId") Long centerId
    );

    /** Owner view: attendance for a class in a date range */
    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.studentUser " +
           "WHERE a.schedule.clazz.id = :classId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "AND a.center.id = :centerId " +
           "ORDER BY a.date, a.studentUser.fullName")
    List<Attendance> findAllByClassIdAndDateBetween(
            @Param("classId") Long classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("centerId") Long centerId
    );
}
