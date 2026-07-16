package com.owlexa.owlexabackend.modules.teacher_attendance.dto.request;

import com.owlexa.owlexabackend.modules.teacher_attendance.entity.TeacherAttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAttendanceMarkRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    @Valid
    private List<Item> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @NotNull
        private Long teacherUserId;

        @NotNull
        private TeacherAttendanceStatus status;

        private String note;
    }
}
