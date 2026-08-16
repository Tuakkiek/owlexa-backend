package com.owlexa.owlexabackend.modules.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private long totalTransactions;
    private BigDecimal totalRevenue;
    private long pendingCount;
}
