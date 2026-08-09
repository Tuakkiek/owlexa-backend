package com.owlexa.owlexabackend.modules.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDueDateRequest {

    @NotNull(message = "Hạn đóng học phí không được để trống")
    private LocalDate dueDate;
}
