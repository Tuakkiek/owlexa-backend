package com.owlexa.owlexabackend.modules.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Published by {@link com.owlexa.owlexabackend.modules.ai_scoring.service.AiScoringService}
 * after all rubric criteria for a single essay question submission have been scored by AI.
 * <p>
 * The {@link com.owlexa.owlexabackend.modules.analytics.listener.AnalyticsEventProcessor}
 * listens for this event to update AI drift analytics.
 */
@Getter
@AllArgsConstructor
public class AiScoringCompletedEvent {
    /** The HomeworkQuestionSubmission that was AI-scored. */
    private final Long questionSubmissionId;
    private final Long homeworkId;
    private final Long classId;
    private final Long centerId;
}
