package com.owlexa.owlexabackend.modules.teacher_attendance.dto.response;

import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAttendanceResponse {

    private Long id;
    private Long centerId;

    private Long scheduleEventId;
    private Long classId;
    private String className;
    private Long roomId;
    private String roomName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String eventStatus;

    private Long teacherUserId;
    private String teacherFullName;
    private String teacherPhoneNumber;

    private LocalDate date;
    private TeacherAttendanceStatus status;
    private String note;

    private Long markedByUserId;
    private LocalDateTime createdAt;
}
