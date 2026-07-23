package com.owlexa.owlexabackend.modules.class_management.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String name;

    private Long courseId;

    @Min(value = 1, message = "Sĩ số tối đa phải ít nhất là 1")
    private Integer maxStudent;

    @Min(value = 0, message = "Học phí hàng tháng không được âm")
    private Double monthlyFee;

    private com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus status;
}
