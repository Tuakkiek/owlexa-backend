package com.owlexa.owlexabackend.modules.ai_grading.repository;

import com.owlexa.owlexabackend.modules.ai_grading.entity.AIGradingItemResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AIGradingItemResultRepository extends JpaRepository<AIGradingItemResult, Long> {

    List<AIGradingItemResult> findAllByResult_Id(Long resultId);

    Optional<AIGradingItemResult> findByResult_IdAndSubmissionAnswer_Id(Long resultId, Long submissionAnswerId);
}
