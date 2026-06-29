package com.owlexa.owlexabackend.modules.auth.dto.response;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {

    private String sessionId;

    private String deviceName;
    private DeviceType deviceType;
    private String ipAddress;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    private boolean current;
}