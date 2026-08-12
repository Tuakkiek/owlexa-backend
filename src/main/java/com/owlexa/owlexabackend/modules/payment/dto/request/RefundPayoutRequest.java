package com.owlexa.owlexabackend.modules.payment.dto.request;

import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundPayoutRequest {
    @NotNull
    private PaymentMethod refundMethod;
}
