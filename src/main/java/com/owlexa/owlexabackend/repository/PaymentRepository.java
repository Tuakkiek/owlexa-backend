package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByFeeRecordOrderByCreatedAtDesc(Long feeRecordId);

    List<Payment> findAllByCenterIdOrderByCreatedAtDesc(Long centerId);

    List<Payment> findAllByStudentUserIdOrderByCreatedAtDesc(Long studentUserId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId")
    BigDecimal sumAmountByCenterId(@Param("centerId") Long centerId);

}
