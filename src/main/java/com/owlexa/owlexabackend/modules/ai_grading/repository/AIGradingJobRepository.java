package com.owlexa.owlexabackend.modules.ai_grading.repository;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJob;
import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIGradingJobRepository extends JpaRepository<AIGradingJob, Long> {

    Optional<AIGradingJob> findByActiveJobKey(Long activeJobKey);

    Optional<AIGradingJob> findTopBySubmissionAttempt_IdOrderByCreatedAtDesc(Long submissionAttemptId);

    List<AIGradingJob> findAllBySubmissionAttempt_IdOrderByCreatedAtDesc(Long submissionAttemptId);

    Page<AIGradingJob> findAllByStatus(AIGradingJobStatus status, Pageable pageable);

    Optional<AIGradingJob> findByIdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
            Long id,
            Long centerId
    );

    Optional<AIGradingJob> findByIdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_CreatedBy_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
            Long id,
            Long centerId,
            Long teacherUserId
    );
}
