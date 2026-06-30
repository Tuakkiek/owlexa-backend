package com.owlexa.owlexabackend.modules.attendance.dto.response;

import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private LocalDate date;
    private AttendanceStatus status;
    private String note;

    private Long markedByUserId;
    private LocalDateTime createdAt;
}
