package com.owlexa.owlexabackend.modules.class_management.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterRequest {
    @NotBlank(message = "Tên trung tâm không được để trống")
    private String name;

    @NotBlank(message = "Tên miền phụ không được để trống")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tên miền phụ chỉ cho phép chữ cái thường, số và dấu gạch ngang")
    private String subdomain;
}
