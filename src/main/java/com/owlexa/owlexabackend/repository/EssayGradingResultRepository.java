package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.EssayGradingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EssayGradingResultRepository extends JpaRepository<EssayGradingResult, Long> {
    Optional<EssayGradingResult> findBySubmissionId(Long submissionId);
}
