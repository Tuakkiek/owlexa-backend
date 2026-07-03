package com.owlexa.owlexabackend.modules.payment.repository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByFeeRecordOrderByCreatedAtDesc(FeeRecord feeRecord);

    List<Payment> findAllByCenter_IdOrderByCreatedAtDesc(Long centerId);

    List<Payment> findAllByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.center.id = :centerId")
    BigDecimal sumAmountByCenterId(@Param("centerId") Long centerId);

}
