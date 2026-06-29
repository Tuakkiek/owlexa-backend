package com.owlexa.owlexabackend.modules.mocktest.repository;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestQuestionRepository extends JpaRepository<MockTestQuestion, Long> {
    List<MockTestQuestion> findAllByMockTestIdOrderBySortOrderAscIdAsc(Long mockTestId);

    Optional<MockTestQuestion> findByIdAndMockTestId(Long id, Long mockTestId);

    void deleteAllByMockTestId(Long mockTestId);
}
