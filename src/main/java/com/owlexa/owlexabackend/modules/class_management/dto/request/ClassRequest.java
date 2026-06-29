package com.owlexa.owlexabackend.modules.class_management.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRequest {

    @NotBlank(message = "Class name is required")
    private String name;

    @NotBlank(message = "Vstep level is required")
    private String vstepLevel;

    @NotNull(message = "Max student is required")
    @Min(value = 1, message = "Max student must be at least 1")
    private Integer maxStudent;

    @NotNull(message = "Month fee is required")
    @Min(value = 0, message = "Month fee cannot be negative")
    private Double monthlyFee;
}
