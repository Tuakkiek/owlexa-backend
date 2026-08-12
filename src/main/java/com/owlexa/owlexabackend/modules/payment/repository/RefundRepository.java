package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.Refund;
import com.owlexa.owlexabackend.modules.payment.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByPaymentOrderByCreatedAtDesc(Payment payment);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.center.id = :centerId")
    BigDecimal sumAmountByCenterId(@Param("centerId") Long centerId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status = 'PAID'")
    BigDecimal sumPaidAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status NOT IN ('REJECTED')")
    BigDecimal sumNonRejectedAmountByPaymentId(@Param("paymentId") Long paymentId);

    List<Refund> findAllByCenter_IdAndStatusOrderByCreatedAtDesc(Long centerId, RefundStatus status);

    List<Refund> findAllByCenter_IdOrderByCreatedAtDesc(Long centerId);
}

