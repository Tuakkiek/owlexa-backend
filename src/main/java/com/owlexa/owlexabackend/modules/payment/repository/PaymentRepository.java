package com.owlexa.owlexabackend.modules.payment.repository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findAllByFeeRecordOrderByCreatedAtDesc(FeeRecord feeRecord);

    List<Payment> findAllByCenter_IdOrderByCreatedAtDesc(Long centerId);

    List<Payment> findAllByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

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

}
