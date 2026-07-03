package com.owlexa.owlexabackend.modules.mocktest.repository;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttempt;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, Long> {
    List<MockTestAttempt> findAllByMockTest_IdOrderByStartedAtDesc(Long mockTestId);

    List<MockTestAttempt> findAllByStudentUser_IdOrderByStartedAtDesc(Long studentUserId);

    List<MockTestAttempt> findAllByStudentUser_IdAndStatusOrderByStartedAtDesc(Long studentUserId, MockTestAttemptStatus status);

    List<MockTestAttempt> findAllByStudentUser_IdInAndCenter_IdAndStatusOrderByStartedAtDesc(
            List<Long> studentUserIds,
            Long centerId,
            MockTestAttemptStatus status
    );

    Optional<MockTestAttempt> findTopByStudentUser_IdAndMockTest_IdAndStatusOrderByStartedAtDesc(Long studentUserId, Long mockTestId, MockTestAttemptStatus status);
}
