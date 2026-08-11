package com.owlexa.owlexabackend.modules.auth.dto.request;
import lombok.Data;

@Data
@Deprecated(since = "2.0.0", forRemoval = true)
public class RefreshTokenRequest {

    @Deprecated
    private String refreshToken;
}
