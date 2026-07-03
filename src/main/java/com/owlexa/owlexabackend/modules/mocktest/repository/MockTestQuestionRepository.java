package com.owlexa.owlexabackend.modules.mocktest.repository;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockTestQuestionRepository extends JpaRepository<MockTestQuestion, Long> {
    List<MockTestQuestion> findAllByMockTest_IdOrderBySortOrderAscIdAsc(Long mockTestId);

    Optional<MockTestQuestion> findByIdAndMockTest_Id(Long id, Long mockTestId);

    void deleteAllByMockTest_Id(Long mockTestId);
}
