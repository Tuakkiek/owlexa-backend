package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SePayWebhookEventRepository extends JpaRepository<SePayWebhookEvent, Long> {
    Optional<SePayWebhookEvent> findBySepayTransactionId(Long sepayTransactionId);
    boolean existsBySepayTransactionId(Long sepayTransactionId);

    @Query("SELECT e, p FROM SePayWebhookEvent e, Payment p " +
           "WHERE e.matchedPaymentId = p.id " +
           "AND e.processingStatus = com.owlexa.owlexabackend.modules.payment.entity.SePayEventStatus.DUPLICATE_PAYMENT " +
           "AND p.center.id = :centerId")
    List<Object[]> findDuplicatePaymentRowsByCenterId(@Param("centerId") Long centerId);

    @Query("SELECT e, p FROM SePayWebhookEvent e, Payment p " +
           "WHERE e.matchedPaymentId = p.id " +
           "AND e.processingStatus = com.owlexa.owlexabackend.modules.payment.entity.SePayEventStatus.DUPLICATE_PAYMENT " +
           "AND p.studentUser.id = :studentUserId")
    List<Object[]> findDuplicatePaymentRowsByStudentUserId(@Param("studentUserId") Long studentUserId);
}
