package com.owlexa.owlexabackend.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminStatusRequest(
        @NotNull(message = "Trạng thái hoạt động là bắt buộc") Boolean active,
        @NotBlank(message = "Lý do thay đổi trạng thái là bắt buộc")
        @Size(min = 3, max = 500, message = "Lý do phải từ 3 đến 500 ký tự") String reason
) {
}
