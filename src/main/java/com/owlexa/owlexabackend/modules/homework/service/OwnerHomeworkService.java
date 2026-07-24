package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkTemplate;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkTemplateRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerHomeworkService {

    private final HomeworkTemplateRepository templateRepository;
    private final HomeworkAssignmentRepository assignmentRepository;
    private final HomeworkSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public Page<HomeworkTemplate> getAllTemplates(String keyword, com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type, Pageable pageable) {
        Long centerId = TenantContext.getCurrentTenantId();
        // Fallback to in-memory filtering for simplicity, ideally should use Specification
        List<HomeworkTemplate> all = templateRepository.findAllByCenter_Id(centerId);
        List<HomeworkTemplate> filtered = all.stream()
                .filter(t -> keyword == null || keyword.isEmpty() || t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .filter(t -> type == null || t.getHomeworkType() == type)
                .collect(java.util.stream.Collectors.toList());
                
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        if (start > filtered.size()) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, filtered.size());
        }
        return new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }
    
    @Transactional(readOnly = true)
    public HomeworkTemplate getTemplateDetail(Long templateId) {
        Long centerId = TenantContext.getCurrentTenantId();
        return templateRepository.findByIdAndCenter_Id(templateId, centerId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<HomeworkAssignment> getAllAssignments(String keyword, Long classId, Long teacherId, com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus status, Pageable pageable) {
        Long centerId = TenantContext.getCurrentTenantId();
        List<HomeworkAssignment> all = assignmentRepository.findAllByCenter_Id(centerId);
        List<HomeworkAssignment> filtered = all.stream()
                .filter(a -> keyword == null || keyword.isEmpty() || a.getHomeworkTemplate().getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .filter(a -> classId == null || a.getClazz().getId().equals(classId))
                .filter(a -> teacherId == null || a.getTeacher().getId().equals(teacherId))
                .filter(a -> status == null || a.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
                
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        if (start > filtered.size()) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, filtered.size());
        }
        return new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }
    
    @Transactional(readOnly = true)
    public HomeworkAssignment getAssignmentDetail(Long assignmentId) {
        Long centerId = TenantContext.getCurrentTenantId();
        return assignmentRepository.findByIdAndCenter_Id(assignmentId, centerId).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public Page<com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission> getSubmissionsForAssignment(Long assignmentId, Pageable pageable) {
        Long centerId = TenantContext.getCurrentTenantId();
        // First verify assignment belongs to center
        HomeworkAssignment assignment = assignmentRepository.findByIdAndCenter_Id(assignmentId, centerId).orElse(null);
        if (assignment == null) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
        }
        
        // Fetch submissions
        List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission> all = submissionRepository.findAllByHomeworkAssignment_Id(assignmentId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), all.size());
        if (start > all.size()) {
            return new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, all.size());
        }
        return new org.springframework.data.domain.PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
