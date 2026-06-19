package com.owlexa.owlexabackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRecordGenerateRequest {

    @NotBlank(message = "month is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "month must have format YYYY-MM")
    private String month;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;
}