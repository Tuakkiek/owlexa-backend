package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest;
import com.owlexa.owlexabackend.modules.payment.entity.SePayEventStatus;
import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import com.owlexa.owlexabackend.modules.payment.repository.SePayWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayWebhookService {

    private final SePayWebhookEventRepository eventRepository;
    private final SePayCodeResolver codeResolver;
    private final PaymentService paymentService;

    @Transactional
    public SePayWebhookEvent processWebhook(SePayWebhookRequest req, String rawBody) {
        // 1. Build event and attempt to persist immediately.
        //    This acts as the idempotency guard: the UNIQUE constraint on
        //    sepay_transaction_id ensures only one thread can insert.
        SePayWebhookEvent event = buildEvent(req, rawBody);

        try {
            event = eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException e) {
            // Duplicate — another thread already processed this transaction
            log.info("SePay webhook id={} already persisted (duplicate key), retrieving existing", req.getId());
            return eventRepository.findBySepayTransactionId(req.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Duplicate key but existing event not found for sepayTransactionId=" + req.getId()));
        }

        // 2. Only "in" transfers are relevant for fee collection
        if (!"in".equalsIgnoreCase(req.getTransferType())) {
            event.setProcessingStatus(SePayEventStatus.IGNORED);
            event.setProcessingNote("transferType is not 'in'");
            event.setProcessedAt(LocalDateTime.now());
            return eventRepository.save(event);
        }

        // 3. Resolve payment code -> paymentId
        // Primary: use SePay-extracted code field
        // Fallback: scan raw transfer content for the OWX prefix pattern
        // (banking apps often modify the transfer content, so code may be null)
        Optional<Long> paymentIdOpt = codeResolver.resolvePaymentId(req.getCode());
        if (paymentIdOpt.isEmpty() && req.getContent() != null && !req.getContent().isBlank()) {
            log.debug("[SEPAY-WEBHOOK] code field is null/blank, scanning content for payment code");
            paymentIdOpt = codeResolver.resolveFromContent(req.getContent());
        }
        if (paymentIdOpt.isEmpty()) {
            event.setProcessingStatus(SePayEventStatus.UNMATCHED);
            event.setProcessingNote("Could not resolve payment code: " + req.getCode());
            event.setProcessedAt(LocalDateTime.now());
            log.warn("[SEPAY-WEBHOOK] SePay webhook id={} unmatched: code={}, content={}",
                    event.getSepayTransactionId(), event.getPaymentCode(), event.getContent());
            return eventRepository.save(event);
        }

        Long paymentId = paymentIdOpt.get();
        event.setMatchedPaymentId(paymentId);
        // ── TEMPORARY DEBUG LOGGING ──
        log.debug("[SEPAY-WEBHOOK] Payment code resolved: code='{}' -> paymentId={}",
                req.getCode(), paymentId);
        // ── END TEMPORARY DEBUG ──

        // 4. Confirm the pending payment via PaymentService
        try {
            // ── TEMPORARY DEBUG LOGGING ──
            log.debug("[SEPAY-WEBHOOK] Attempting to confirm paymentId={}", paymentId);
            // ── END TEMPORARY DEBUG ──

            paymentService.confirmBankTransferPayment(paymentId, req);

            event.setProcessingStatus(SePayEventStatus.MATCHED);
            event.setProcessingNote("Payment confirmed successfully");
            // ── TEMPORARY DEBUG LOGGING ──
            log.debug("[SEPAY-WEBHOOK] CONFIRMED: paymentId={} for sepayTxId={}",
                    paymentId, event.getSepayTransactionId());
            // ── END TEMPORARY DEBUG ──
        } catch (Exception e) {
            log.error("[SEPAY-WEBHOOK] Failed to process SePay webhook id={} for paymentId={}",
                    req.getId(), paymentId, e);
            event.setProcessingStatus(SePayEventStatus.FAILED);
            event.setProcessingNote("Error: " + e.getMessage());
        }

        event.setProcessedAt(LocalDateTime.now());
        return eventRepository.save(event);
    }

    private SePayWebhookEvent buildEvent(SePayWebhookRequest req, String rawBody) {
        SePayWebhookEvent event = new SePayWebhookEvent();
        event.setSepayTransactionId(req.getId());
        event.setReferenceCode(req.getReferenceCode());
        event.setGateway(req.getGateway());
        event.setAccountNumber(req.getAccountNumber());
        event.setSubAccount(req.getSubAccount());
        event.setPaymentCode(req.getCode());
        event.setContent(req.getContent());
        event.setTransferType(req.getTransferType());
        event.setTransferAmount(req.getTransferAmount());
        event.setTransactionDateRaw(req.getTransactionDate());
        event.setRawPayload(rawBody);
        event.setReceivedAt(LocalDateTime.now());
        event.setProcessingStatus(SePayEventStatus.RECEIVED);
        return event;
    }
}
