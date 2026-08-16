package com.owlexa.owlexabackend.modules.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionResponse {
    private Long scheduleEventId;
    private Long classId;
    private String className;
    private Long teacherUserId;
    private String teacherUserFullName;
    private Long roomId;
    private String roomName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    
    // Attendance Stats
    private String attendanceStatus; // "COMPLETED" or "PENDING"
    private int studentCount;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private int excusedCount;
}
