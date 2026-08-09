package com.owlexa.owlexabackend.modules.class_management.repository;

import com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingTimeSlotRepository extends JpaRepository<TeachingTimeSlot, Long> {

    List<TeachingTimeSlot> findAllByCenter_IdOrderByDisplayOrderAscStartTimeAsc(Long centerId);

    List<TeachingTimeSlot> findAllByCenter_IdAndIsActiveTrueOrderByDisplayOrderAscStartTimeAsc(Long centerId);

    Optional<TeachingTimeSlot> findByIdAndCenter_Id(Long id, Long centerId);

    @Query("SELECT t FROM TeachingTimeSlot t WHERE t.center.id = :centerId AND t.isActive = true AND (:excludeId IS NULL OR t.id <> :excludeId) AND t.startTime < :endTime AND t.endTime > :startTime")
    List<TeachingTimeSlot> findOverlappingActiveSlots(
            @Param("centerId") Long centerId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );
}
