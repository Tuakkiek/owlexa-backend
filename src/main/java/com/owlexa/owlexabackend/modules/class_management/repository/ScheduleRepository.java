package com.owlexa.owlexabackend.modules.class_management.repository;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByClazz_IdAndCenter_Id(Long classId, Long centerId);

    List<Schedule> findAllByTeacherUser_IdAndCenter_Id(Long teacherUserId, Long centerId);

    boolean existsByClazz_IdAndTeacherUser_IdAndCenter_Id(Long clazzId, Long teacherUserId, Long centerId);

    List<Schedule> findAllByCenter_Id(Long centerId);

    boolean existsByClazz_IdAndDayOfWeekAndStartTimeAndCenter_Id(
            Long classId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            Long centerId
    );

    @Query("SELECT s FROM Schedule s WHERE s.teacherUser.id = :teacherId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    List<Schedule> findOverlappingTeacherSchedules(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId,
            @Param("excludeId") Long excludeScheduleId);

    @Query("SELECT s FROM Schedule s WHERE s.room.id = :roomId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    List<Schedule> findOverlappingRoomSchedules(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId,
            @Param("excludeId") Long excludeScheduleId);

    @Query("SELECT s FROM Schedule s WHERE s.clazz.id IN " +
           "(SELECT e.clazz.id FROM ClassEnrollment e WHERE e.studentUser.id = :studentId AND e.status IN (com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE, com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING, com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED)) " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)")
    List<Schedule> findOverlappingStudentSchedules(
            @Param("studentId") Long studentId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId,
            @Param("excludeScheduleId") Long excludeScheduleId);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.teacherUser.id = :teacherId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    long countOverlappingTeacherSchedules(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId,
            @Param("excludeId") Long excludeScheduleId);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.room.id = :roomId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    long countOverlappingRoomSchedules(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId,
            @Param("excludeId") Long excludeScheduleId);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.clazz.id IN " +
           "(SELECT e.clazz.id FROM ClassEnrollment e WHERE e.studentUser.id = :studentId AND e.status = 'ACTIVE') " +
           "AND s.dayOfWeek = :dayOfWeek AND s.type <> com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType.CANCELLED AND s.center.id = :centerId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    long countOverlappingStudentSchedules(
            @Param("studentId") Long studentId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("centerId") Long centerId);

    List<Schedule> findAllByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    boolean existsByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    @Query("""
            SELECT MIN(s.room.capacity) FROM Schedule s
            WHERE s.clazz.id = :classId
              AND s.center.id = :centerId
              AND s.room IS NOT NULL
              AND s.room.capacity IS NOT NULL
            """)
    Integer findMinRoomCapacityByClass(
            @Param("classId") Long classId,
            @Param("centerId") Long centerId
    );

    void deleteByClazz_IdAndCenter_Id(Long classId, Long centerId);

    void deleteByCenter_Id(Long centerId);
}

