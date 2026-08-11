package com.owlexa.owlexabackend.modules.class_management.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long id;
    private Long classId;
    private String className;
    private Long centerId;

    private Long teacherUserId;
    private String teacherUserFullName;
    private String teacherPhoneNumber;

    private Long roomId;
    private String roomName;
    private String roomCode;

    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    private ScheduleType type;
    private LocalDate eventDate;
    private Integer lessonNumber;
    private ScheduleEventStatus eventStatus;
    private String source;
    private Instant createdAt;
}
