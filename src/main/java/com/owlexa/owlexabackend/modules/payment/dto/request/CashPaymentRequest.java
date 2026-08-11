package com.owlexa.owlexabackend.modules.payment.dto.request;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashPaymentRequest {

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @Builder.Default
    private PaymentMethod method = PaymentMethod.CASH;

    private String note;
}
