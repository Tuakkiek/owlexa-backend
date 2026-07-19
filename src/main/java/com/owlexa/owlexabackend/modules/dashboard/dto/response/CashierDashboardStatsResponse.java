package com.owlexa.owlexabackend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierDashboardStatsResponse {

    private long totalPaymentsToday;
    private BigDecimal totalAmountCollectedToday;
    private long totalPendingPayments;
    private BigDecimal totalPendingAmount;
    private long totalPaymentsThisMonth;
}
