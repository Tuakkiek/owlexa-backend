package com.owlexa.owlexabackend.modules.payment.dto.response;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
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

    private Long feeRecordId;
    private Long centerId;
    private Long classId;

    private Long studentUserId;
    private String studentPhoneNumber;
    private String studentFullName;

    private BigDecimal amount;
    private PaymentMethod method;
    private String sepayRef;
    private String note;

    private Long collectedByUserId;
    private Instant createdAt;

    private BigDecimal feeRecordAmount;
    private BigDecimal feeRecordPaidAmount;
    private BigDecimal feeRecordRemainingAmount;
    private FeeStatus feeRecordStatus;

}
