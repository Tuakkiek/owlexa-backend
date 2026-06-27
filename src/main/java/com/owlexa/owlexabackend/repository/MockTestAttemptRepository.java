package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.MockTestAttempt;
import com.owlexa.owlexabackend.entity.MockTestAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, Long> {
    List<MockTestAttempt> findAllByMockTestIdOrderByStartedAtDesc(Long mockTestId);

    List<MockTestAttempt> findAllByStudentUserIdOrderByStartedAtDesc(Long studentUserId);

    List<MockTestAttempt> findAllByStudentUserIdAndStatusOrderByStartedAtDesc(Long studentUserId, MockTestAttemptStatus status);

    List<MockTestAttempt> findAllByStudentUserIdInAndCenterIdAndStatusOrderByStartedAtDesc(
            List<Long> studentUserIds,
            Long centerId,
            MockTestAttemptStatus status
    );

    Optional<MockTestAttempt> findTopByStudentUserIdAndMockTestIdAndStatusOrderByStartedAtDesc(Long studentUserId, Long mockTestId, MockTestAttemptStatus status);
}
