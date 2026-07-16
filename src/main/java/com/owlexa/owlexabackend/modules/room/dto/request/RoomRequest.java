package com.owlexa.owlexabackend.modules.room.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    private String description;

    private Boolean isActive;
}
