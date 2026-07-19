package com.owlexa.owlexabackend.modules.payment.dto.request;

import com.owlexa.owlexabackend.modules.payment.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
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
public class DiscountRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "type is required")
    private DiscountType type;

    @NotNull(message = "value is required")
    @Positive(message = "value must be positive")
    private BigDecimal value;

    private String reason;
}
