package com.owlexa.owlexabackend.modules.class_management.dto.response;

import com.owlexa.owlexabackend.modules.class_management.entity.TimeSlotPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingTimeSlotResponse {

    private Long id;
    private Long centerId;
    private String name;
    private TimeSlotPeriod period;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isUsed;
}
