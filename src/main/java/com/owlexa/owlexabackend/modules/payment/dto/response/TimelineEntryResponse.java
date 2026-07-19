package com.owlexa.owlexabackend.modules.payment.dto.response;

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
public class TimelineEntryResponse {

    private Instant timestamp;
    private String action;
    private String userName;
    private String description;
    private BigDecimal amount;
    private Long entityId;
    private String receiptNumber;
}
