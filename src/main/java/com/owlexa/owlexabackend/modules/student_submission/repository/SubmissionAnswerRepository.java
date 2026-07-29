package com.owlexa.owlexabackend.modules.student_submission.repository;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, Long> {

    List<SubmissionAnswer> findAllByAttempt_Id(Long attemptId);

    Optional<SubmissionAnswer> findByAttempt_IdAndAssignmentItem_Id(Long attemptId, Long assignmentItemId);
}
