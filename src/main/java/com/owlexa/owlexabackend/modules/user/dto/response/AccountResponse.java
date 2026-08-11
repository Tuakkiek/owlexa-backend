package com.owlexa.owlexabackend.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Hồ sơ tài khoản của chính người dùng đang đăng nhập (GET /account).
 * Cố tình giữ shape gần giống {@code AuthResponse} (trừ accessToken) để FE
 * tái dùng cùng một kiểu dữ liệu UserInfo.
 */
@Data
@Builder
public class AccountResponse {

    private Long userId;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String roleName;
    private String centerName;
    private Long centerId;
    private java.util.List<String> permissions;
}
