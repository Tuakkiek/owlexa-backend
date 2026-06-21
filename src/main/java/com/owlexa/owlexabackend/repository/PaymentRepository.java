package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByFeeRecordOrderByCreatedAtDesc(Long feeRecordId);

    List<Payment> findAllByCenterIdOrderByCreatedAtDesc(Long centerId);

    List<Payment> findAllByStudentUserIdOrderByCreatedAtDesc(Long studentUserId);

}
