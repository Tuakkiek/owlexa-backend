package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;
    private Long scheduleId;
    private Long classId;
    private Long centerId;

    private Long studentUserId;
    private String studentPhoneNumber;
    private String studentFullName;

    private LocalDate sessionDate;
    private AttendanceStatus status;
    private String note;

    private Long notedByUserId;
    private Instant createdAt;
}
