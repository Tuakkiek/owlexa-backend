package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long id;
    private Long classId;
    private Long centerId;

    private Long teacherUserId;
    private String teacherUserFullName;
    private String teacherPhoneNumber;

    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;

    private boolean isActive;
    private Instant createAt;
}
