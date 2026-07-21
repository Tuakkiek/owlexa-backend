package com.owlexa.owlexabackend.common.scheduler;

import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically expires PENDING bank transfer payments
 * that have passed their expiresAt timestamp.
 *
 * Runs at a configurable fixed rate (default: every 5 minutes).
 * Only transitions PENDING → EXPIRED; ACTIVE payments are never touched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationJob {

    private final PaymentRepository paymentRepository;

    /**
     * Runs every 5 minutes. Finds all PENDING payments past their expiresAt
     * and transitions them to EXPIRED.
     */
    @Scheduled(fixedRateString = "${app.payment.expiration-check-interval-ms:300000}")
    @Transactional
    public void expirePendingPayments() {
        Instant now = Instant.now();

        List<Payment> expiredPayments = paymentRepository
                .findAllByStatusAndExpiresAtBefore(TransactionStatus.PENDING, now);

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Expiring {} pending bank transfer payments", expiredPayments.size());
        for (Payment payment : expiredPayments) {
            payment.setStatus(TransactionStatus.EXPIRED);
            paymentRepository.save(payment);
            log.debug("Expired payment id={}, sepayRef={}, expired at {}",
                    payment.getId(), payment.getSepayRef(), payment.getExpiresAt());
        }
    }
}
