package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.DeviceType;
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