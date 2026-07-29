package com.owlexa.owlexabackend.modules.ai_grading.repository;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface AIGradingResultRepository extends JpaRepository<AIGradingResult, Long> {

    Optional<AIGradingResult> findByJob_Id(Long jobId);

    Optional<AIGradingResult> findTopBySubmissionAttempt_IdAndJob_StatusOrderByCreatedAtDesc(
            Long submissionAttemptId,
            AIGradingJobStatus status
    );

    Optional<AIGradingResult> findTopBySubmissionAttempt_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNullAndJob_StatusOrderByCreatedAtDesc(
            Long submissionAttemptId,
            Long centerId,
            AIGradingJobStatus status
    );

    @Query("""
            SELECT result
            FROM AIGradingResult result
            WHERE result.id = :resultId
              AND result.submissionAttempt.id = :submissionAttemptId
              AND result.submissionAttempt.assignmentRecipient.assignment.center.id = :centerId
              AND result.submissionAttempt.assignmentRecipient.assignment.deletedAt IS NULL
              AND result.job.status = :status
            """)
    Optional<AIGradingResult> findSelectableResult(
            @Param("resultId") Long resultId,
            @Param("submissionAttemptId") Long submissionAttemptId,
            @Param("centerId") Long centerId,
            @Param("status") AIGradingJobStatus status
    );

    @Query("""
            SELECT DISTINCT result.submissionAttempt.id
            FROM AIGradingResult result
            WHERE result.submissionAttempt.id IN :submissionAttemptIds
              AND result.submissionAttempt.assignmentRecipient.assignment.center.id = :centerId
              AND result.submissionAttempt.assignmentRecipient.assignment.deletedAt IS NULL
              AND result.job.status = :status
            """)
    Set<Long> findAttemptIdsWithResult(
            @Param("submissionAttemptIds") Collection<Long> submissionAttemptIds,
            @Param("centerId") Long centerId,
            @Param("status") AIGradingJobStatus status
    );
}
