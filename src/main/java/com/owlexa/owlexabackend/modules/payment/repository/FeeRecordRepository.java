package com.owlexa.owlexabackend.modules.payment.repository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findAllByClazz_IdAndMonth(Long classId, String month);

    List<FeeRecord> findAllByCenter_IdAndMonth(Long centerId, String month);

    List<FeeRecord> findAllByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

    List<FeeRecord> findAllByCenter_IdAndStatusAndDueDateBefore(
            Long centerId,
            FeeStatus status,
            LocalDate dueDate
    );

    Optional<FeeRecord> findByStudentUser_IdAndClazz_IdAndMonth(
            Long studentUserId,
            Long classId,
            String month
    );

    boolean existsByClazz_IdAndMonth(Long classId, String month);

    long countByCenter_Id(Long centerId);

    long countByCenter_IdAndStatus(Long centerId, FeeStatus status);

    void deleteByCenter_Id(Long centerId);
}