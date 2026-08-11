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

    @NotBlank(message = "Tên chiết khấu không được để trống")
    private String name;

    @NotNull(message = "Loại chiết khấu không được để trống")
    private DiscountType type;

    @NotNull(message = "Giá trị chiết khấu không được để trống")
    @Positive(message = "Giá trị chiết khấu phải lớn hơn 0")
    private BigDecimal value;

    private String reason;
}
