package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.FeeRecord;
import com.owlexa.owlexabackend.entity.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findAllByClazzIdAndMonth(Long classId, String month);

    List<FeeRecord> findAllByCenterIdAndMonth(Long centerId, String month);

    List<FeeRecord> findAllByStudentUserIdOrderByCreatedAtDesc(Long studentUserId);

    List<FeeRecord> findAllByCenterIdAndStatusAndDueDateBefore(
            Long centerId,
            FeeStatus status,
            LocalDate dueDate
    );

    Optional<FeeRecord> findByStudentUserIdAndClazzIdAndMonth(
            Long studentUserId,
            Long classId,
            String month
    );

    boolean existsByClazzIdAndMonth(Long classId, String month);

    long countByCenterId(Long centerId);

    long countByCenterIdAndStatus(Long centerId, FeeStatus status);

    void deleteByCenterId(Long centerId);
}