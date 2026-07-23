package com.owlexa.owlexabackend.modules.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomScheduleSummaryResponse {
    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String className;
    private String teacherName;
    private String type;
}
