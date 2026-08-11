package com.owlexa.owlexabackend.modules.teacher.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSalaryRequest {

    /**
     * Mức lương. Không được âm và không bắt buộc khác 0
     * (cho phép set 0 = tạm thời chưa trả lương cố định).
     *
     * Để xóa salary thì gọi API riêng, không truyền 0.
     */
    @NotNull(message = "Lương không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Lương phải lớn hơn hoặc bằng 0")
    @Digits(integer = 10, fraction = 2, message = "Lương chỉ được chứa tối đa 2 chữ số thập phân")
    private BigDecimal salary;

    /**
     * Đơn vị tiền tệ. Optional — nếu không truyền thì giữ nguyên giá trị hiện tại,
     * nếu chưa có profile thì default "VND".
     *
     * Validate 3 ký tự chữ hoa theo chuẩn ISO 4217 (VND, USD, EUR, ...).
     */
    @Size(min = 3, max = 3, message = "Đơn vị tiền tệ phải đúng 3 ký tự")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Đơn vị tiền tệ phải là mã ISO 4217 hợp lệ (3 chữ cái viết hoa)")
    private String currency;
}