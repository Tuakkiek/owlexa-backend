package com.owlexa.owlexabackend.modules.ai_scoring.repository;

import com.owlexa.owlexabackend.modules.ai_scoring.entity.AiScoringJob;
import com.owlexa.owlexabackend.modules.homework.enums.AiScoringStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiScoringJobRepository extends JpaRepository<AiScoringJob, Long> {

    Optional<AiScoringJob> findTopByQuestionSubIdOrderByCreatedAtDesc(Long questionSubId);

    List<AiScoringJob> findAllByStatus(AiScoringStatus status);

    List<AiScoringJob> findAllByQuestionSubIdOrderByCreatedAtDesc(Long questionSubId);
}
