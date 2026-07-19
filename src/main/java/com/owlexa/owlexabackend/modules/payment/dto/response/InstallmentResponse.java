package com.owlexa.owlexabackend.modules.payment.dto.response;

import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentResponse {

    private Long id;
    private Long feeRecordId;
    private LocalDate dueDate;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private InstallmentStatus status;
}
