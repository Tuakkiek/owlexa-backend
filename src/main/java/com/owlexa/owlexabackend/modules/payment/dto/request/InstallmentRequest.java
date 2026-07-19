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

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    @NotNull(message = "expectedAmount is required")
    @Positive(message = "expectedAmount must be positive")
    private BigDecimal expectedAmount;
}
