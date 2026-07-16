package com.owlexa.owlexabackend.modules.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private Long id;
    private String code;
    private String name;
    private Integer capacity;
    private String description;
    private Boolean isActive;
    private Long centerId;
    private Instant createdAt;
    private Instant updatedAt;
}
