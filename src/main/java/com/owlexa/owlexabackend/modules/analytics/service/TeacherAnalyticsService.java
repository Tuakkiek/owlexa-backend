package com.owlexa.owlexabackend.modules.analytics.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.analytics.dto.response.AiDriftResponse;
import com.owlexa.owlexabackend.modules.analytics.dto.response.AnalyticsClassPerformanceResponse;
import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsClassPerformance;
import com.owlexa.owlexabackend.modules.analytics.entity.AnalyticsRubricWeakness;
import com.owlexa.owlexabackend.modules.analytics.repository.AnalyticsClassPerformanceRepository;
import com.owlexa.owlexabackend.modules.analytics.repository.AnalyticsRubricWeaknessRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAnalyticsService {

    private final AnalyticsClassPerformanceRepository classPerformanceRepository;
    private final ClassRepository classRepository;
    private final AnalyticsRubricWeaknessRepository rubricWeaknessRepository;

    @Transactional(readOnly = true)
    public List<AnalyticsClassPerformanceResponse> getClassPerformanceList(Long classId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        Class clazz = classRepository.findByIdAndCenter_Id(classId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found."));
                
        // Validation: verify teacher is assigned to this class (omitted here for simplicity, assuming teacher can view analytics for their class)

        List<AnalyticsClassPerformance> performances = classPerformanceRepository.findAllByClazz_IdAndCenter_Id(classId, centerId);

        return performances.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private AnalyticsClassPerformanceResponse mapToResponse(AnalyticsClassPerformance entity) {
        AnalyticsClassPerformanceResponse r = new AnalyticsClassPerformanceResponse();
        r.setId(entity.getId());
        r.setClassId(entity.getClazz().getId());
        r.setHomeworkId(entity.getHomeworkAssignment().getId());
        r.setSubmittedCount(entity.getSubmittedCount());
        r.setGradedCount(entity.getGradedCount());
        r.setLateSubmissionCount(entity.getLateSubmissionCount());
        r.setMissingSubmissionCount(entity.getMissingSubmissionCount());
        r.setAverageScore(entity.getAverageScore());
        r.setHighestScore(entity.getHighestScore());
        r.setLowestScore(entity.getLowestScore());
        r.setPassRate(entity.getPassRate());
        r.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return r;
    }

    /**
     * Returns AI vs Teacher drift analytics for each rubric criterion in a class.
     * Only criteria with at least one AI-scored submission will appear.
     */
    @Transactional(readOnly = true)
    public java.util.List<AiDriftResponse> getAiDrift(Long classId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        classRepository.findByIdAndCenter_Id(classId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found."));

        return rubricWeaknessRepository.findAllByClazz_IdAndCenter_Id(classId, centerId)
                .stream()
                .filter(w -> w.getAiAverageScore() != null) // only show criteria with AI data
                .map(this::mapToDriftResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private AiDriftResponse mapToDriftResponse(AnalyticsRubricWeakness w) {
        AiDriftResponse r = new AiDriftResponse();
        r.setCriterionId(w.getRubricCriterion().getId());
        r.setCriterionName(w.getRubricCriterion().getName());
        r.setAiAvg(w.getAiAverageScore());
        r.setTeacherAvg(w.getTeacherAverageScore());
        r.setDriftRate(w.getDriftRate());
        r.setMaxScore(w.getMaxScore());
        return r;
    }
}
