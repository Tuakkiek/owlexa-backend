package com.owlexa.owlexabackend.modules.payment.repository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findAllByFeeRecordOrderByCreatedAtDesc(FeeRecord feeRecord);

    List<Payment> findAllByCenter_IdOrderByCreatedAtDesc(Long centerId);

    List<Payment> findAllByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

    List<Payment> findAllByStatusAndExpiresAtBefore(TransactionStatus status, Instant expiresAt);

    long countByFeeRecord_Clazz_IdAndFeeRecord_Center_IdAndStatus(Long classId, Long centerId, TransactionStatus status);

    void deleteByFeeRecord_Clazz_IdAndFeeRecord_Center_Id(Long classId, Long centerId);

    // ── Idempotency & duplicate prevention ──────────────────────────────

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** Find the single current PENDING payment for a fee record + student (if any). */
    @Query("SELECT p FROM Payment p WHERE p.feeRecord.id = :feeRecordId " +
           "AND p.studentUser.id = :studentUserId AND p.status = 'PENDING' " +
           "AND (p.method = 'BANK_TRANSFER' OR p.method = 'SEPAY' OR p.method = 'QR_CODE')")
    Optional<Payment> findCurrentPendingByFeeRecordAndStudent(
            @Param("feeRecordId") Long feeRecordId,
            @Param("studentUserId") Long studentUserId);

    /** Pessimistic write lock on FeeRecord to prevent race conditions. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fr FROM FeeRecord fr WHERE fr.id = :id")
    Optional<FeeRecord> findFeeRecordByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.status = 'ACTIVE'")
    BigDecimal sumAmountByCenterId(@Param("centerId") Long centerId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.center.id = :centerId AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    long countByCenterIdAndCreatedAtBetween(@Param("centerId") Long centerId,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    BigDecimal sumAmountByCenterIdAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end);

    @Query("SELECT MAX(p.receiptNumber) FROM Payment p WHERE p.receiptNumber LIKE CONCAT(:prefix, '%')")
    String findMaxReceiptNumberByPrefix(@Param("prefix") String prefix);

    // ── Revenue aggregates ──────────────────────────────────────────────────

    @Query("SELECT COALESCE(AVG(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    BigDecimal avgAmountByCenterIdAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end);

    @Query("SELECT COALESCE(MAX(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    BigDecimal maxAmountByCenterIdAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end);

    @Query("SELECT COALESCE(MIN(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    BigDecimal minAmountByCenterIdAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                       @Param("start") Instant start,
                                                       @Param("end") Instant end);


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId AND p.method = :method AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    BigDecimal sumAmountByCenterIdAndMethodAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                                @Param("method") com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod method,
                                                                @Param("start") Instant start,
                                                                @Param("end") Instant end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.center.id = :centerId AND p.method = :method AND p.createdAt >= :start AND p.createdAt < :end AND p.status = 'ACTIVE'")
    long countByCenterIdAndMethodAndCreatedAtBetween(@Param("centerId") Long centerId,
                                                      @Param("method") com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod method,
                                                      @Param("start") Instant start,
                                                      @Param("end") Instant end);

    // ── Drop / Transfer helpers ──────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.studentUser.id = :studentUserId AND p.feeRecord.clazz.id = :classId AND p.status = 'ACTIVE'")
    BigDecimal sumActivePaymentsByStudentAndClass(@Param("studentUserId") Long studentUserId,
                                                   @Param("classId") Long classId);

    @Query("SELECT p FROM Payment p WHERE p.studentUser.id = :studentUserId AND p.feeRecord.clazz.id = :classId AND p.status = 'ACTIVE' ORDER BY p.createdAt DESC")
    List<Payment> findActivePaymentsByStudentAndClass(@Param("studentUserId") Long studentUserId,
                                                       @Param("classId") Long classId);

}
