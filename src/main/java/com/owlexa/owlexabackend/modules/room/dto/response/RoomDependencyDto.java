package com.owlexa.owlexabackend.modules.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDependencyDto {
    private String className;
    private String teacherName;
    private String source;
    private String dayOfWeek;
    private String timeRange;
}
