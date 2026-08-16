package com.owlexa.owlexabackend.modules.student_submission.repository;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubmissionAttemptRepository extends JpaRepository<SubmissionAttempt, Long> {

    Optional<SubmissionAttempt> findByAssignmentRecipient_IdAndStatus(
            Long assignmentRecipientId,
            SubmissionAttemptStatus status
    );

    Optional<SubmissionAttempt> findByActiveAttemptKey(Long activeAttemptKey);

    Optional<SubmissionAttempt> findTopByAssignmentRecipient_IdOrderByAttemptNumberDesc(Long assignmentRecipientId);

    List<SubmissionAttempt> findAllByAssignmentRecipient_IdOrderByAttemptNumberDesc(Long assignmentRecipientId);

    Optional<SubmissionAttempt> findTopByAssignmentRecipient_IdOrderByStartedAtDesc(Long assignmentRecipientId);

    long countByAssignmentRecipient_Id(Long assignmentRecipientId);

    long countByAssignmentRecipient_Assignment_Id(Long assignmentId);

    Optional<SubmissionAttempt> findByIdAndAssignmentRecipient_StudentUser_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
            Long id,
            Long studentUserId,
            Long centerId
    );

    Optional<SubmissionAttempt> findByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
            Long id,
            Long centerId
    );

    boolean existsByIdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_CreatedBy_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
            Long id,
            Long centerId,
            Long teacherUserId
    );

    Page<SubmissionAttempt> findAllByAssignmentRecipient_Assignment_IdAndAssignmentRecipient_Assignment_Center_IdAndAssignmentRecipient_Assignment_DeletedAtIsNull(
            Long assignmentId,
            Long centerId,
            Pageable pageable
    );

    List<SubmissionAttempt> findAllByStatusAndExpiresAtLessThanEqual(
            SubmissionAttemptStatus status,
            Instant expiresAt
    );
}
