package com.owlexa.owlexabackend.modules.payment.dto.response;

import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.payment.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private Long id;
    private Long paymentId;
    private Long centerId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private PaymentMethod refundMethod;

    private Long createdByUserId;
    private String createdByUserName;
    private Instant createdAt;

    private Long requestedByUserId;
    private String requestedByUserName;

    private Long approvedByUserId;
    private String approvedByUserName;
    private Instant approvedAt;

    private String rejectedReason;

    private Long relatedEnrollmentId;

    // Payment context
    private String paymentReceiptNumber;
    private BigDecimal paymentAmount;
    private String studentFullName;
    private String studentPhoneNumber;
}
