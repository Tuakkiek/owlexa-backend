package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.MockTestAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestAttemptAnswerRepository extends JpaRepository<MockTestAttemptAnswer, Long> {
    List<MockTestAttemptAnswer> findAllByAttemptIdOrderByQuestionIdAsc(Long attemptId);

    Optional<MockTestAttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
