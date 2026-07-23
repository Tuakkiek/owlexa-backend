package com.owlexa.owlexabackend.modules.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeworkGradedEvent {
    private final Long homeworkId;
    private final Long classId;
    private final Long centerId;
    private final Long studentId;
    private final Double oldScore; // Can be null if first time
    private final Double newScore;
}
