package com.owlexa.owlexabackend.modules.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sepay_event_id", columnNames = "sepay_transaction_id")
})
@Data
public class SePayWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SePay's own transaction id (payload field "id") — idempotency key
    @Column(name = "sepay_transaction_id", nullable = false, unique = true)
    private Long sepayTransactionId;

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "sub_account")
    private String subAccount;

    @Column(name = "payment_code")
    private String paymentCode;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "transfer_type")
    private String transferType;

    @Column(name = "transfer_amount")
    private Long transferAmount;

    @Column(name = "transaction_date")
    private String transactionDateRaw;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private SePayEventStatus processingStatus;

    @Column(name = "processing_note", length = 1000)
    private String processingNote;

    @Column(name = "matched_payment_id")
    private Long matchedPaymentId;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
