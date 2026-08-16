package com.owlexa.owlexabackend.modules.attendance.dto.response;

import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClassSessionResponse {
    private Long scheduleEventId;
    private Long classId;
    private String className;
    private String roomName;
    private String teacherName;
    private LocalTime startTime;
    private LocalTime endTime;

    private AttendanceStatus attendanceStatus; // null if not marked
    private String note;
}
