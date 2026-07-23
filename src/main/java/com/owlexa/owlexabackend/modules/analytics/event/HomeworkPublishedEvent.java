package com.owlexa.owlexabackend.modules.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeworkPublishedEvent {
    private final Long homeworkId;
    private final Long classId;
    private final Long centerId;
    private final int classSize; // Total students in class at the time of publishing
}
