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

    @NotBlank(message = "fullName is required")
    private String fullName;

    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^0\\d{9}$", message = "phoneNumber must be a valid VN number (10 digits)")
    private String phoneNumber;

    /**
     * Optional permission overrides to apply after user creation/update.
     * When null or empty, the user inherits only role-default permissions.
     */
    private List<PermissionOverrideItem> permissionOverrides;
}
