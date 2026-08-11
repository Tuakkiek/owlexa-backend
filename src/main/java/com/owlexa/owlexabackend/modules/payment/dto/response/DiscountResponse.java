package com.owlexa.owlexabackend.modules.payment.dto.response;

import com.owlexa.owlexabackend.modules.payment.entity.DiscountType;
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
public class DiscountResponse {

    private Long id;
    private Long feeRecordId;
    private String name;
    private DiscountType type;
    private BigDecimal value;
    private String reason;
    private Long createdByUserId;
    private String createdByUserName;
    private Instant createdAt;
}
