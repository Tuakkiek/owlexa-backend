package com.owlexa.owlexabackend.modules.class_management.repository;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, Long> {
    List<ScheduleEvent> findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(Long classId, Long centerId);

    List<ScheduleEvent> findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(Long teacherUserId, Long centerId);

    List<ScheduleEvent> findAllByCenter_IdOrderByEventDateAscStartTimeAsc(Long centerId);

    List<ScheduleEvent> findAllByCenter_IdAndEventDateAndStatusNotOrderByStartTimeAsc(Long centerId, LocalDate eventDate, ScheduleEventStatus status);

    List<ScheduleEvent> findAllByRoom_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(Long roomId, Long centerId);

    List<ScheduleEvent> findAllByRecurringRule_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(Long ruleId, Long centerId);

    boolean existsByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    long countByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    boolean existsByRecurringRule_IdAndEventDateAndStartTimeAndCenter_Id(Long ruleId, java.time.LocalDate eventDate, java.time.LocalTime startTime, Long centerId);

    boolean existsByRecurringRule_IdAndEventDateAndCenter_Id(Long ruleId, LocalDate eventDate, Long centerId);

    @Query("""
            SELECT MIN(e.room.capacity) FROM ScheduleEvent e
            WHERE e.clazz.id = :classId
              AND e.center.id = :centerId
              AND e.room IS NOT NULL
              AND e.room.capacity IS NOT NULL
            """)
    Integer findMinRoomCapacityByClass(
            @Param("classId") Long classId,
            @Param("centerId") Long centerId
    );

    void deleteByClazz_IdAndCenter_Id(Long classId, Long centerId);

    @Query("""
            SELECT e FROM ScheduleEvent e
            WHERE e.center.id = :centerId
              AND e.clazz.id = :classId
              AND e.eventDate = :eventDate
              AND e.status <> :cancelledStatus
              AND e.startTime < :endTime
              AND e.endTime > :startTime
              AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
            ORDER BY e.startTime ASC
            """)
    List<ScheduleEvent> findOverlappingClassEvents(
            @Param("centerId") Long centerId,
            @Param("classId") Long classId,
            @Param("eventDate") LocalDate eventDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ScheduleEventStatus cancelledStatus,
            @Param("excludeEventId") Long excludeEventId
    );

    @Query("""
            SELECT e FROM ScheduleEvent e
            WHERE e.center.id = :centerId
              AND e.room.id = :roomId
              AND e.eventDate = :eventDate
              AND e.status <> :cancelledStatus
              AND e.startTime < :endTime
              AND e.endTime > :startTime
              AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
            ORDER BY e.startTime ASC
            """)
    List<ScheduleEvent> findOverlappingRoomEvents(
            @Param("centerId") Long centerId,
            @Param("roomId") Long roomId,
            @Param("eventDate") LocalDate eventDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ScheduleEventStatus cancelledStatus,
            @Param("excludeEventId") Long excludeEventId
    );

    @Query("""
            SELECT e FROM ScheduleEvent e
            WHERE e.center.id = :centerId
              AND e.teacherUser.id = :teacherId
              AND e.eventDate = :eventDate
              AND e.status <> :cancelledStatus
              AND e.startTime < :endTime
              AND e.endTime > :startTime
              AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
            ORDER BY e.startTime ASC
            """)
    List<ScheduleEvent> findOverlappingTeacherEvents(
            @Param("centerId") Long centerId,
            @Param("teacherId") Long teacherId,
            @Param("eventDate") LocalDate eventDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ScheduleEventStatus cancelledStatus,
            @Param("excludeEventId") Long excludeEventId
    );

    @Query("""
            SELECT e FROM ScheduleEvent e
            WHERE e.center.id = :centerId
              AND e.clazz.id IN (
                    SELECT ce.clazz.id FROM ClassEnrollment ce
                    WHERE ce.studentUser.id = :studentId
                      AND ce.status IN (
                            com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE,
                            com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING,
                            com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED
                      )
              )
              AND e.eventDate = :eventDate
              AND e.status <> :cancelledStatus
              AND e.startTime < :endTime
              AND e.endTime > :startTime
              AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
            ORDER BY e.startTime ASC
            """)
    List<ScheduleEvent> findOverlappingStudentEvents(
            @Param("centerId") Long centerId,
            @Param("studentId") Long studentId,
            @Param("eventDate") LocalDate eventDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelledStatus") ScheduleEventStatus cancelledStatus,
            @Param("excludeEventId") Long excludeEventId
    );
}
