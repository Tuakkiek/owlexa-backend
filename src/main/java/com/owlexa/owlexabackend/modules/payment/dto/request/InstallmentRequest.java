package com.owlexa.owlexabackend.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class InstallmentRequest {

    @NotNull(message = "Ngày hạn thanh toán không được để trống")
    private LocalDate dueDate;

    @NotNull(message = "Số tiền kỳ hạn không được để trống")
    @Positive(message = "Số tiền kỳ hạn phải lớn hơn 0")
    private BigDecimal expectedAmount;
}
