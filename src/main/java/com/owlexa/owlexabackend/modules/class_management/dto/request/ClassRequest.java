package com.owlexa.owlexabackend.modules.class_management.dto.request;

import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRequest {

    @NotBlank(message = "Class name is required")
    private String name;

    private Long courseId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long teacherUserId;

    @Min(value = 0, message = "Monthly fee cannot be negative")
    private Double monthlyFee;

    private ClassStatus status;
}
