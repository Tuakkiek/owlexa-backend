package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SePayWebhookEventRepository extends JpaRepository<SePayWebhookEvent, Long> {
    Optional<SePayWebhookEvent> findBySepayTransactionId(Long sepayTransactionId);
    boolean existsBySepayTransactionId(Long sepayTransactionId);
}
