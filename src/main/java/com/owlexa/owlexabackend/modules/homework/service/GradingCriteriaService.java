package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherGradingCriteriaSaveRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.TeacherGradingCriteriaResponse;
import com.owlexa.owlexabackend.modules.homework.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.homework.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkTemplateRepository;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class GradingCriteriaService {

    private final GradingCriteriaRepository gradingCriteriaRepository;
    private final HomeworkTemplateRepository homeworkTemplateRepository;
    private final AuthorizationService authService;
    private final EntityManager entityManager;

    public Page<TeacherGradingCriteriaResponse> getCriteriaList(String keyword, Pageable pageable) {
        Long centerId = com.owlexa.owlexabackend.common.context.TenantContext.getCurrentTenantId();
        Page<GradingCriteria> criteriaPage = gradingCriteriaRepository.findByCenterIdAndArchivedAndKeyword(
                centerId,
                false,
                keyword,
                pageable
        );
        return criteriaPage.map(this::mapToResponse);
    }

    public TeacherGradingCriteriaResponse getCriteriaById(Long id) {
        Long centerId = com.owlexa.owlexabackend.common.context.TenantContext.getCurrentTenantId();
        GradingCriteria criteria = getCriteria(id, centerId);
        return mapToResponse(criteria);
    }

    @Transactional
    public TeacherGradingCriteriaResponse createCriteria(TeacherGradingCriteriaSaveRequest request) {
        User currentUser = authService.getCurrentUser();
        Long centerId = com.owlexa.owlexabackend.common.context.TenantContext.getCurrentTenantId();
        
        GradingCriteria criteria = GradingCriteria.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .teacher(currentUser)
                .center(entityManager.getReference(com.owlexa.owlexabackend.modules.user.entity.Center.class, centerId))
                .build();
                
        GradingCriteria saved = gradingCriteriaRepository.save(criteria);
        return mapToResponse(saved);
    }

    @Transactional
    public TeacherGradingCriteriaResponse updateCriteria(Long id, TeacherGradingCriteriaSaveRequest request) {
        Long centerId = com.owlexa.owlexabackend.common.context.TenantContext.getCurrentTenantId();
        GradingCriteria criteria = getCriteria(id, centerId);
        
        criteria.setTitle(request.getTitle());
        criteria.setContent(request.getContent());
        
        GradingCriteria saved = gradingCriteriaRepository.save(criteria);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCriteria(Long id) {
        Long centerId = com.owlexa.owlexabackend.common.context.TenantContext.getCurrentTenantId();
        GradingCriteria criteria = getCriteria(id, centerId);
        
        if (homeworkTemplateRepository.existsByGradingCriteriaId(id)) {
            throw new BusinessRuleException("Không thể xóa tiêu chí này vì đang được sử dụng trong một bài tập. Vui lòng gỡ bỏ hoặc xóa bài tập trước.");
        }
        
        gradingCriteriaRepository.delete(criteria);
    }

    private GradingCriteria getCriteria(Long id, Long centerId) {
        GradingCriteria criteria = gradingCriteriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grading criteria not found"));
                
        if (!criteria.getCenter().getId().equals(centerId)) {
            throw new ResourceNotFoundException("Grading criteria not found");
        }
        
        return criteria;
    }

    private TeacherGradingCriteriaResponse mapToResponse(GradingCriteria criteria) {
        return TeacherGradingCriteriaResponse.builder()
                .id(criteria.getId())
                .title(criteria.getTitle())
                .content(criteria.getContent())
                .createdAt(criteria.getCreatedAt())
                .updatedAt(criteria.getUpdatedAt())
                .build();
    }
}
