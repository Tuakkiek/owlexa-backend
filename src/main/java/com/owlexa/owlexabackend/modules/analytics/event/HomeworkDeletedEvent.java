package com.owlexa.owlexabackend.modules.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeworkDeletedEvent {
    private final Long homeworkId;
    private final Long classId;
    private final Long centerId;
}
