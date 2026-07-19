package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByPaymentOrderByCreatedAtDesc(Payment payment);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.center.id = :centerId")
    BigDecimal sumAmountByCenterId(@Param("centerId") Long centerId);
}
