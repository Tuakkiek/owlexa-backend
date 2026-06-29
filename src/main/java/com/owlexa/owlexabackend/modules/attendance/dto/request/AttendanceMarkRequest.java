package com.owlexa.owlexabackend.modules.attendance.dto.request;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
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
public class AttendanceMarkRequest {

    @NotNull(message = "sessionDate is required")
    private LocalDate sessionDate;

    @NotNull(message = "records is required")
    @Valid
    private List<Item> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @NotNull(message = "studentUserId is required")
        private Long studentUserId;

        @NotNull(message = "status is required")
        private AttendanceStatus status;

        private String note;
    }
}