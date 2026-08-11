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

    @NotBlank(message = "Mã phòng học không được để trống")
    private String code;

    @NotBlank(message = "Tên phòng học không được để trống")
    private String name;

    @Min(value = 1, message = "Sức chứa của phòng phải ít nhất là 1")
    private Integer capacity;

    private String description;

    private Boolean isActive;
}
