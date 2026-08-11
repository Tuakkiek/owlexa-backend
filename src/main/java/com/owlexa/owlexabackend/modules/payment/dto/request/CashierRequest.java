package com.owlexa.owlexabackend.modules.payment.dto.request;

import com.owlexa.owlexabackend.modules.user.dto.request.PermissionOverrideItem;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0 (VD: 0912345678)")
    private String phoneNumber;

    /**
     * Optional permission overrides to apply after user creation/update.
     * When null or empty, the user inherits only role-default permissions.
     */
    private List<PermissionOverrideItem> permissionOverrides;
}
