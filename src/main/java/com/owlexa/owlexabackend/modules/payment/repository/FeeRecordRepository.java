package com.owlexa.owlexabackend.modules.payment.repository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findAllByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

    List<FeeRecord> findAllByStudentUser_IdAndClazz_Id(Long studentUserId, Long classId);

    @Query("SELECT fr FROM FeeRecord fr WHERE fr.studentUser.id = :studentUserId " +
           "AND fr.clazz.id IN (" +
           "  SELECT e.clazz.id FROM ClassEnrollment e " +
           "  WHERE e.studentUser.id = :studentUserId " +
           "  AND e.status IN (com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE, " +
           "                  com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING, " +
           "                  com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED)" +
           ") " +
           "ORDER BY fr.createdAt DESC")
    List<FeeRecord> findAllActiveEnrollmentFeesByStudentUserId(@Param("studentUserId") Long studentUserId);

    List<FeeRecord> findAllByCenter_IdAndStatusAndDueDateBefore(
            Long centerId,
            FeeStatus status,
            LocalDate dueDate
    );

    List<FeeRecord> findAllByStudentUser_IdAndClazz_IdAndMonthOrderByIdDesc(
            Long studentUserId, Long classId, String month);

    default Optional<FeeRecord> findByStudentUser_IdAndClazz_IdAndMonth(
            Long studentUserId, Long classId, String month) {
        return findAllByStudentUser_IdAndClazz_IdAndMonthOrderByIdDesc(studentUserId, classId, month).stream()
                .sorted(Comparator
                        .comparingInt((FeeRecord record) -> statusPriority(record.getStatus()))
                        .thenComparing(FeeRecord::getPaidAmount, Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(FeeRecord::getId, Comparator.reverseOrder()))
                .findFirst();
    }

    private static int statusPriority(FeeStatus status) {
        return switch (status) {
            case PAID -> 0;
            case PARTIAL -> 1;
            case OVERDUE -> 2;
            case UNPAID -> 3;
            case CANCELLED -> 4;
        };
    }

    List<FeeRecord> findAllByStatusAndDueDateBefore(FeeStatus status, LocalDate date);

    @Query("""
            SELECT fr FROM FeeRecord fr
            WHERE fr.status IN :statuses
              AND fr.dueDate < :dueDate
              AND EXISTS (
                  SELECT e.id FROM ClassEnrollment e
                  WHERE e.clazz.id = fr.clazz.id
                    AND e.studentUser.id = fr.studentUser.id
                    AND e.status IN (
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED
                    )
              )
            """)
    List<FeeRecord> findAllByStatusInAndDueDateBefore(@Param("statuses") List<FeeStatus> statuses,
                                                       @Param("dueDate") LocalDate dueDate);

    @Query("""
            SELECT COUNT(fr) FROM FeeRecord fr
            WHERE fr.studentUser.id = :studentUserId
              AND fr.clazz.id = :classId
              AND fr.status IN :statuses
              AND (fr.dueDate IS NULL OR fr.dueDate <= :asOfDate)
            """)
    long countOutstandingDueByStudentAndClass(
            @Param("studentUserId") Long studentUserId,
            @Param("classId") Long classId,
            @Param("statuses") List<FeeStatus> statuses,
            @Param("asOfDate") LocalDate asOfDate);

    boolean existsByStudentUser_IdAndClazz_IdAndStatusAndDueDateBefore(
            Long studentUserId, Long classId, FeeStatus status, LocalDate date);

    long countByCenter_Id(Long centerId);

    long countByCenter_IdAndStatus(Long centerId, FeeStatus status);

    long countByClazz_IdAndCenter_Id(Long classId, Long centerId);

    @Query("""
            SELECT COUNT(fr) FROM FeeRecord fr
            WHERE fr.clazz.id = :classId
              AND fr.center.id = :centerId
              AND (
                fr.status IN (com.owlexa.owlexabackend.modules.payment.entity.FeeStatus.PAID,
                              com.owlexa.owlexabackend.modules.payment.entity.FeeStatus.PARTIAL)
                OR COALESCE(fr.paidAmount, 0) > 0
              )
            """)
    long countSettledRecordsByClass(
            @Param("classId") Long classId,
            @Param("centerId") Long centerId
    );

    void deleteByClazz_IdAndCenter_Id(Long classId, Long centerId);

    void deleteByCenter_Id(Long centerId);

    @Query("SELECT COALESCE(SUM(fr.amount - COALESCE(fr.paidAmount, 0)), 0) FROM FeeRecord fr WHERE fr.center.id = :centerId AND fr.status = :status")
    BigDecimal sumRemainingByCenterIdAndStatus(@Param("centerId") Long centerId,
                                                @Param("status") FeeStatus status);

    // ── Multi-status queries for unpaid list (UNPAID + PARTIAL) ──────────────

    @Query("""
            SELECT fr FROM FeeRecord fr
            WHERE fr.center.id = :centerId
              AND fr.status IN :statuses
              AND fr.dueDate < :dueDate
              AND EXISTS (
                  SELECT e.id FROM ClassEnrollment e
                  WHERE e.clazz.id = fr.clazz.id
                    AND e.studentUser.id = fr.studentUser.id
                    AND e.status IN (
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED
                    )
              )
            """)
    List<FeeRecord> findAllByCenter_IdAndStatusInAndDueDateBefore(@Param("centerId") Long centerId,
                                                                   @Param("statuses") List<FeeStatus> statuses,
                                                                   @Param("dueDate") LocalDate dueDate);

    @Query("SELECT COUNT(fr) FROM FeeRecord fr WHERE fr.center.id = :centerId AND fr.status IN :statuses")
    long countByCenter_IdAndStatusIn(@Param("centerId") Long centerId,
                                     @Param("statuses") List<FeeStatus> statuses);

    @Query("SELECT COALESCE(SUM(fr.amount - COALESCE(fr.paidAmount, 0)), 0) FROM FeeRecord fr WHERE fr.center.id = :centerId AND fr.status IN :statuses")
    BigDecimal sumRemainingByCenterIdAndStatusIn(@Param("centerId") Long centerId,
                                                  @Param("statuses") List<FeeStatus> statuses);

    // ── Pending fees (UNPAID + PARTIAL, regardless of due date) ──────────

    @Query("""
            SELECT fr FROM FeeRecord fr
            WHERE fr.center.id = :centerId
              AND fr.status IN :statuses
              AND EXISTS (
                  SELECT e.id FROM ClassEnrollment e
                  WHERE e.clazz.id = fr.clazz.id
                    AND e.studentUser.id = fr.studentUser.id
                    AND e.status IN (
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.PENDING,
                        com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.SUSPENDED
                    )
              )
            ORDER BY fr.createdAt DESC
            """)
    List<FeeRecord> findAllByCenter_IdAndStatusInOrderByCreatedAtDesc(
            @Param("centerId") Long centerId, @Param("statuses") List<FeeStatus> statuses);

    List<FeeRecord> findAllByCenter_IdAndClazz_IdOrderByCreatedAtDesc(Long centerId, Long classId);
}
