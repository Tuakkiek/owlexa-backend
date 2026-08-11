package com.owlexa.owlexabackend.modules.teacher_review.repository;

import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReview;
import com.owlexa.owlexabackend.modules.teacher_review.entity.TeacherReviewStatus;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeacherReviewRepository extends JpaRepository<TeacherReview, Long> {

    Optional<TeacherReview> findBySubmissionAttempt_Id(Long submissionAttemptId);

    boolean existsBySubmissionAttempt_Id(Long submissionAttemptId);

    @EntityGraph(attributePaths = {
            "items",
            "items.assignmentItem",
            "items.submissionAnswer",
            "submissionAttempt",
            "selectedAiGradingResult",
            "createdBy",
            "updatedBy",
            "finalizedBy",
            "releasedBy"
    })
    @Query("""
            SELECT review
            FROM TeacherReview review
            WHERE review.id = :reviewId
              AND review.submissionAttempt.assignmentRecipient.assignment.center.id = :centerId
              AND review.submissionAttempt.assignmentRecipient.assignment.deletedAt IS NULL
            """)
    Optional<TeacherReview> findDetailByIdAndCenterId(
            @Param("reviewId") Long reviewId,
            @Param("centerId") Long centerId
    );

    @EntityGraph(attributePaths = {
            "items",
            "items.assignmentItem",
            "items.submissionAnswer",
            "submissionAttempt",
            "selectedAiGradingResult",
            "createdBy",
            "updatedBy",
            "finalizedBy",
            "releasedBy"
    })
    @Query("""
            SELECT review
            FROM TeacherReview review
            WHERE review.submissionAttempt.id = :submissionAttemptId
              AND review.submissionAttempt.assignmentRecipient.assignment.center.id = :centerId
              AND review.submissionAttempt.assignmentRecipient.assignment.deletedAt IS NULL
            """)
    Optional<TeacherReview> findDetailBySubmissionAttemptIdAndCenterId(
            @Param("submissionAttemptId") Long submissionAttemptId,
            @Param("centerId") Long centerId
    );

    @EntityGraph(attributePaths = {
            "items",
            "items.assignmentItem",
            "submissionAttempt",
            "selectedAiGradingResult"
    })
    @Query("""
            SELECT review
            FROM TeacherReview review
            WHERE review.submissionAttempt.id = :submissionAttemptId
              AND review.status = :status
              AND review.submissionAttempt.assignmentRecipient.studentUser.id = :studentUserId
              AND review.submissionAttempt.assignmentRecipient.assignment.center.id = :centerId
              AND review.submissionAttempt.assignmentRecipient.assignment.deletedAt IS NULL
            """)
    Optional<TeacherReview> findReleasedDetailForStudent(
            @Param("submissionAttemptId") Long submissionAttemptId,
            @Param("studentUserId") Long studentUserId,
            @Param("centerId") Long centerId,
            @Param("status") TeacherReviewStatus status
    );

    Page<TeacherReview> findAllBySubmissionAttempt_AssignmentRecipient_Assignment_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_Center_IdAndSubmissionAttempt_AssignmentRecipient_Assignment_DeletedAtIsNull(
            Long assignmentId,
            Long centerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"selectedAiGradingResult"})
    List<TeacherReview> findAllBySubmissionAttempt_IdIn(Collection<Long> submissionAttemptIds);

    @Query(
            value = """
                    SELECT attempt
                    FROM SubmissionAttempt attempt
                    JOIN FETCH attempt.assignmentRecipient recipient
                    JOIN FETCH recipient.assignment assignment
                    JOIN FETCH recipient.studentUser
                    LEFT JOIN FETCH recipient.clazz
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                    """,
            countQuery = """
                    SELECT COUNT(attempt)
                    FROM SubmissionAttempt attempt
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                    """
    )
    Page<SubmissionAttempt> findReviewQueueAttempts(
            @Param("assignmentId") Long assignmentId,
            @Param("centerId") Long centerId,
            @Param("submissionStatuses") Collection<SubmissionAttemptStatus> submissionStatuses,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT attempt
                    FROM SubmissionAttempt attempt
                    JOIN FETCH attempt.assignmentRecipient recipient
                    JOIN FETCH recipient.assignment assignment
                    JOIN FETCH recipient.studentUser
                    LEFT JOIN FETCH recipient.clazz
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                      AND NOT EXISTS (
                          SELECT review.id
                          FROM TeacherReview review
                          WHERE review.submissionAttempt = attempt
                      )
                    """,
            countQuery = """
                    SELECT COUNT(attempt)
                    FROM SubmissionAttempt attempt
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                      AND NOT EXISTS (
                          SELECT review.id
                          FROM TeacherReview review
                          WHERE review.submissionAttempt = attempt
                      )
                    """
    )
    Page<SubmissionAttempt> findUnreviewedQueueAttempts(
            @Param("assignmentId") Long assignmentId,
            @Param("centerId") Long centerId,
            @Param("submissionStatuses") Collection<SubmissionAttemptStatus> submissionStatuses,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT attempt
                    FROM SubmissionAttempt attempt
                    JOIN FETCH attempt.assignmentRecipient recipient
                    JOIN FETCH recipient.assignment assignment
                    JOIN FETCH recipient.studentUser
                    LEFT JOIN FETCH recipient.clazz
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                      AND EXISTS (
                          SELECT review.id
                          FROM TeacherReview review
                          WHERE review.submissionAttempt = attempt
                            AND review.status = :reviewStatus
                      )
                    """,
            countQuery = """
                    SELECT COUNT(attempt)
                    FROM SubmissionAttempt attempt
                    WHERE attempt.assignmentRecipient.assignment.id = :assignmentId
                      AND attempt.assignmentRecipient.assignment.center.id = :centerId
                      AND attempt.assignmentRecipient.assignment.deletedAt IS NULL
                      AND attempt.status IN :submissionStatuses
                      AND EXISTS (
                          SELECT review.id
                          FROM TeacherReview review
                          WHERE review.submissionAttempt = attempt
                            AND review.status = :reviewStatus
                      )
                    """
    )
    Page<SubmissionAttempt> findQueueAttemptsByReviewStatus(
            @Param("assignmentId") Long assignmentId,
            @Param("centerId") Long centerId,
            @Param("submissionStatuses") Collection<SubmissionAttemptStatus> submissionStatuses,
            @Param("reviewStatus") TeacherReviewStatus reviewStatus,
            Pageable pageable
    );
}
