package com.owlexa.owlexabackend.modules.payment.dto.response;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
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
public class PaymentResponse {

    private Long id;
    private String receiptNumber;

    private Long feeRecordId;
    private Long centerId;
    private Long classId;
    private String className;
    private String courseName;

    private Long studentUserId;
    private String studentPhoneNumber;
    private String studentFullName;

    private BigDecimal amount;
    private PaymentMethod method;
    private String sepayRef;
    private String note;

    private Long collectedByUserId;
    private String collectedByUserName;
    private String centerName;
    private TransactionStatus status;
    private Instant createdAt;
    private Instant expiresAt;

    private BigDecimal feeRecordAmount;
    private BigDecimal feeRecordPaidAmount;
    private BigDecimal feeRecordRemainingAmount;
    private FeeStatus feeRecordStatus;

}
