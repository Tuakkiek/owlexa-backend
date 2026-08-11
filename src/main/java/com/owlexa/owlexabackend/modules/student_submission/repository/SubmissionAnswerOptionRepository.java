package com.owlexa.owlexabackend.modules.student_submission.repository;

import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionAnswerOptionRepository extends JpaRepository<SubmissionAnswerOption, Long> {

    List<SubmissionAnswerOption> findAllBySubmissionAnswer_Id(Long submissionAnswerId);
}
