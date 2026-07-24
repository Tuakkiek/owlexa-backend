package com.owlexa.owlexabackend.modules.analytics.listener;

import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsClassPerformance;
import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsRubricWeakness;
import com.owlexa.owlexabackend.modules.analytics.event.*;
import com.owlexa.owlexabackend.modules.analytics.repository.AnalyticsClassPerformanceRepository;
import com.owlexa.owlexabackend.modules.analytics.repository.AnalyticsRubricWeaknessRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionSubmission;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterionScore;
import com.owlexa.owlexabackend.modules.homework.enums.GraderType;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkQuestionSubmissionRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventProcessor {

    private final AnalyticsClassPerformanceRepository performanceRepository;
    private final AnalyticsRubricWeaknessRepository weaknessRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final ClassRepository classRepository;
    private final HomeworkQuestionSubmissionRepository questionSubmissionRepository;

    @Async
    @EventListener
    @Transactional
    public void handleHomeworkPublished(HomeworkPublishedEvent event) {
        log.info("Initializing analytics for Homework Assignment ID: {}", event.getHomeworkId());
        
        AnalyticsClassPerformance performance = new AnalyticsClassPerformance();
        performance.setClazz(classRepository.findById(event.getClassId()).orElseThrow());
        performance.setHomeworkAssignment(homeworkAssignmentRepository.findById(event.getHomeworkId()).orElseThrow());
        performance.setCenter(performance.getClazz().getCenter());
        performance.setMissingSubmissionCount(event.getClassSize());
        performance.setUpdatedAt(Instant.now());
        
        performanceRepository.save(performance);
    }

    @Async
    @EventListener
    @Transactional
    public void handleHomeworkSubmitted(HomeworkSubmittedEvent event) {
        log.info("Processing submission analytics for Homework Assignment ID: {}", event.getHomeworkId());

        AnalyticsClassPerformance performance = performanceRepository
                .findByClazz_IdAndHomeworkAssignment_IdAndCenter_Id(event.getClassId(), event.getHomeworkId(), event.getCenterId())
                .orElse(null);

        if (performance == null) return; // Should not happen if published properly
        
        performance.setSubmittedCount(performance.getSubmittedCount() + 1);
        if (performance.getMissingSubmissionCount() > 0) {
            performance.setMissingSubmissionCount(performance.getMissingSubmissionCount() - 1);
        }
        
        if (event.isLate()) {
            performance.setLateSubmissionCount(performance.getLateSubmissionCount() + 1);
        }
        
        // Update pass rate
        updatePassRate(performance);

        performance.setUpdatedAt(Instant.now());
        performanceRepository.save(performance);
    }

    @Async
    @EventListener
    @Transactional
    public void handleHomeworkGraded(HomeworkGradedEvent event) {
        log.info("Processing grading analytics for Homework Assignment ID: {}", event.getHomeworkId());

        AnalyticsClassPerformance performance = performanceRepository
                .findByClazz_IdAndHomeworkAssignment_IdAndCenter_Id(event.getClassId(), event.getHomeworkId(), event.getCenterId())
                .orElse(null);

        if (performance == null) return;

        double oldScore = event.getOldScore() != null ? event.getOldScore() : 0.0;
        double newScore = event.getNewScore() != null ? event.getNewScore() : 0.0;
        int count = performance.getGradedCount();

        if (event.getOldScore() == null) {
            // First time graded
            performance.setGradedCount(count + 1);
            count++;
            
            double newAvg = ((performance.getAverageScore() * (count - 1)) + newScore) / count;
            performance.setAverageScore(newAvg);
        } else {
            // Override/Update
            if (count > 0) {
                double newAvg = ((performance.getAverageScore() * count) - oldScore + newScore) / count;
                performance.setAverageScore(newAvg);
            }
        }
        
        // Update Highest / Lowest
        if (performance.getHighestScore() == null || newScore > performance.getHighestScore()) {
            performance.setHighestScore(newScore);
        }
        
        if (performance.getLowestScore() == null || newScore < performance.getLowestScore()) {
            performance.setLowestScore(newScore);
        }

        updatePassRate(performance);
        
        performance.setUpdatedAt(Instant.now());
        performanceRepository.save(performance);
    }

    @Async
    @EventListener
    @Transactional
    public void handleHomeworkReturned(HomeworkReturnedEvent event) {
        log.info("Processing returned analytics for Homework Assignment ID: {}", event.getHomeworkId());

        AnalyticsClassPerformance performance = performanceRepository
                .findByClazz_IdAndHomeworkAssignment_IdAndCenter_Id(event.getClassId(), event.getHomeworkId(), event.getCenterId())
                .orElse(null);

        if (performance == null) return;
        
        int count = performance.getGradedCount();
        if (count > 0) {
            double oldScore = event.getOldScore() != null ? event.getOldScore() : 0.0;
            if (count == 1) {
                performance.setAverageScore(0.0);
            } else {
                double newAvg = ((performance.getAverageScore() * count) - oldScore) / (count - 1);
                performance.setAverageScore(newAvg);
            }
            performance.setGradedCount(count - 1);
            updatePassRate(performance);
            performance.setUpdatedAt(Instant.now());
            performanceRepository.save(performance);
        }
    }
    
    @Async
    @EventListener
    @Transactional
    public void handleHomeworkDeleted(HomeworkDeletedEvent event) {
        performanceRepository.findByClazz_IdAndHomeworkAssignment_IdAndCenter_Id(event.getClassId(), event.getHomeworkId(), event.getCenterId())
                .ifPresent(performanceRepository::delete);
    }

    // ── AI Drift Analytics ────────────────────────────────────────────────

    /**
     * Updates AI drift analytics when AI scoring completes for a question submission.
     * For each rubric criterion in the submission, updates the rolling AI average score
     * in {@link com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsRubricWeakness}
     * and recomputes driftRate if teacher scores are also available.
     */
    @Async
    @EventListener
    @Transactional
    public void handleAiScoringCompleted(AiScoringCompletedEvent event) {
        log.info("[Analytics] Processing AI drift for questionSubmissionId={}", event.getQuestionSubmissionId());

        HomeworkQuestionSubmission qs = questionSubmissionRepository
                .findWithRubricDetailsById(event.getQuestionSubmissionId())
                .orElse(null);

        if (qs == null) {
            log.warn("[Analytics] QuestionSubmission {} not found for drift update.", event.getQuestionSubmissionId());
            return;
        }

        List<HomeworkRubricCriterionScore> aiScores = qs.getCriterionScores().stream()
                .filter(cs -> cs.getGraderType() == GraderType.AI)
                .toList();

        for (HomeworkRubricCriterionScore aiScore : aiScores) {
            Long criterionId = aiScore.getCriterion().getId();

            AnalyticsRubricWeakness weakness = weaknessRepository
                    .findByClazz_IdAndRubricCriterion_IdAndCenter_Id(event.getClassId(), criterionId, event.getCenterId())
                    .orElse(null);

            if (weakness == null) {
                log.debug("[Analytics] No AnalyticsRubricWeakness found for criterion {} in class {}. Skipping drift update.",
                        criterionId, event.getClassId());
                continue;
            }

            // Update rolling AI average
            int submissionCount = weakness.getSubmissionCount();
            double currentAiAvg = weakness.getAiAverageScore() != null ? weakness.getAiAverageScore() : 0.0;
            double newAiAvg;

            if (submissionCount == 0) {
                newAiAvg = aiScore.getScore() != null ? aiScore.getScore() : 0.0;
            } else {
                newAiAvg = ((currentAiAvg * submissionCount) + (aiScore.getScore() != null ? aiScore.getScore() : 0.0))
                        / (submissionCount + 1);
            }
            weakness.setAiAverageScore(newAiAvg);

            // Recompute drift rate if teacher average is also available
            if (weakness.getTeacherAverageScore() != null && weakness.getMaxScore() != null && weakness.getMaxScore() > 0) {
                double drift = Math.abs(newAiAvg - weakness.getTeacherAverageScore()) / weakness.getMaxScore();
                weakness.setDriftRate(Math.min(drift, 1.0)); // cap at 100%
            }

            weakness.setUpdatedAt(Instant.now());
            weaknessRepository.save(weakness);
        }
    }
    
    private void updatePassRate(AnalyticsClassPerformance performance) {
        if (performance.getSubmittedCount() == 0) {
            performance.setPassRate(0.0);
            return;
        }
        // Assuming pass is >= 50% of max score. We don't have maxScore in this table easily, 
        // so we'd need to fetch homework to get maxScore if we want to calculate pass_rate strictly.
        // For O(1) performance, let's defer passRate logic to complex analytics jobs or add maxScore to AnalyticsClassPerformance.
    }
}
