package com.owlexa.owlexabackend.modules.class_management.repository;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRecurringRuleRepository extends JpaRepository<ScheduleRecurringRule, Long> {
    List<ScheduleRecurringRule> findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(Long classId, Long centerId);

    List<ScheduleRecurringRule> findAllByTeacherUser_IdAndCenter_Id(Long teacherUserId, Long centerId);

    List<ScheduleRecurringRule> findAllByRoom_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(Long roomId, Long centerId);

    List<ScheduleRecurringRule> findAllByCenter_IdAndIsActiveTrue(Long centerId);

    boolean existsByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    long countByRoom_IdAndCenter_Id(Long roomId, Long centerId);

    @Query("""
            SELECT MIN(r.room.capacity) FROM ScheduleRecurringRule r
            WHERE r.clazz.id = :classId
              AND r.center.id = :centerId
              AND r.room IS NOT NULL
              AND r.room.capacity IS NOT NULL
            """)
    Integer findMinRoomCapacityByClass(
            @Param("classId") Long classId,
            @Param("centerId") Long centerId
    );

    void deleteByClazz_IdAndCenter_Id(Long classId, Long centerId);
}
