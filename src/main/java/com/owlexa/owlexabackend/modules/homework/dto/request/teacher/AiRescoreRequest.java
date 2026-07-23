package com.owlexa.owlexabackend.modules.homework.dto.request.teacher;

import lombok.Data;

import java.util.List;

/**
 * Request body for the AI re-score endpoint.
 * Specifies which question submissions should be re-evaluated by AI.
 */
@Data
public class AiRescoreRequest {
    /** IDs of HomeworkQuestionSubmission records to re-score. */
    private List<Long> questionSubmissionIds;
}
