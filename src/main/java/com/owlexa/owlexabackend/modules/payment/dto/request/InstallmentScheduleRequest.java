package com.owlexa.owlexabackend.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentScheduleRequest {

    @NotNull(message = "Danh sách kỳ hạn không được để trống")
    private List<InstallmentRequest> installments;
}
