package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionSubmission;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkQuestionSubmissionRepository extends JpaRepository<HomeworkQuestionSubmission, Long> {

    /**
     * Loads a question submission with its question, rubric, criteria, and existing criterion scores
     * in a single query — used by AiScoringService.
     */
    @EntityGraph(attributePaths = {
            "question",
            "question.rubric",
            "question.rubric.criteria",
            "criterionScores"
    })
    Optional<HomeworkQuestionSubmission> findWithRubricDetailsById(Long id);

    /**
     * Returns all question submissions for a given homework submission, filtered by question type.
     * Used by AiScoringEventListener to find only ESSAY submissions.
     */
    List<HomeworkQuestionSubmission> findBySubmission_IdAndQuestion_Type(Long submissionId, HomeworkQuestionType type);

    /** Returns all question submissions for a homework submission. */
    List<HomeworkQuestionSubmission> findBySubmission_Id(Long submissionId);
}
