package com.owlexa.owlexabackend.modules.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeworkSubmittedEvent {
    private final Long homeworkId;
    private final Long classId;
    private final Long centerId;
    private final Long studentId;
    private final boolean isLate;
    /** The specific HomeworkSubmission.id that was just submitted. */
    private final Long submissionId;
}
