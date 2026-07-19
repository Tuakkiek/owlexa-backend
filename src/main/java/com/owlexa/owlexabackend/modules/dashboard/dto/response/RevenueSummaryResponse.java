package com.owlexa.owlexabackend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryResponse {

    private BigDecimal todayRevenue;
    private BigDecimal yesterdayRevenue;
    private BigDecimal thisWeekRevenue;
    private BigDecimal thisMonthRevenue;

    private BigDecimal grossRevenue;
    private BigDecimal discountTotal;
    private BigDecimal refundTotal;
    private BigDecimal netRevenue;

    private BigDecimal outstandingTuition;
    private BigDecimal overdueTuition;

    private long todayTransactionCount;
    private long thisMonthTransactionCount;

    private BigDecimal averagePaymentAmount;
    private BigDecimal highestPaymentAmount;
    private BigDecimal lowestPaymentAmount;

    /** Payment method → total amount breakdown (e.g. "CASH" → 5000000) */
    private Map<String, BigDecimal> methodBreakdown;

    /** Payment method → transaction count */
    private Map<String, Long> methodCounts;
}
