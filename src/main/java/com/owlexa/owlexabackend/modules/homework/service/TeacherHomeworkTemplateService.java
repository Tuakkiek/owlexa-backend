package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.homework.dto.request.*;
import com.owlexa.owlexabackend.modules.homework.entity.*;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkTemplateRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;

@Service
@RequiredArgsConstructor
public class TeacherHomeworkTemplateService {

    private final HomeworkTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final HomeworkAssignmentRepository assignmentRepository;

    @Transactional
    public void saveHomeworkTemplate(Long teacherId, Long templateId, TeacherHomeworkTemplateSaveRequest request) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkTemplate template;
        if (templateId == null) {
            template = new HomeworkTemplate();
            com.owlexa.owlexabackend.modules.user.entity.Center center = new com.owlexa.owlexabackend.modules.user.entity.Center();
            center.setId(centerId);
            template.setCenter(center);
            template.setTeacher(userRepository.findById(teacherId).orElseThrow());
            template.setVersion(1);
        } else {
            template = templateRepository.findWithDetailsByIdAndCenter_IdAndTeacher_Id(templateId, centerId, teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found or access denied."));
                    
            if (assignmentRepository.existsByHomeworkTemplate_Id(templateId)) {
                // Template is already assigned. Create a new version.
                HomeworkTemplate newVersion = new HomeworkTemplate();
                newVersion.setCenter(template.getCenter());
                newVersion.setTeacher(template.getTeacher());
                newVersion.setParentTemplateId(template.getId());
                newVersion.setVersion(template.getVersion() + 1);
                
                // Hide old version from library
                template.setArchived(true);
                templateRepository.save(template);
                
                template = newVersion;
            } else {
                template.getQuestions().clear(); 
            }
        }

        template.setTitle(request.getTitle());
        template.setDescription(request.getDescription());
        template.setInstructions(request.getInstructions());
        template.setHomeworkType(request.getHomeworkType());
        template.setEstimatedTime(request.getEstimatedTime());
        template.setDifficulty(request.getDifficulty());
        template.setMaxScore(request.getMaxScore());

        if (request.getQuestions() != null) {
            for (TeacherHomeworkQuestionRequest qRequest : request.getQuestions()) {
                HomeworkQuestion question = new HomeworkQuestion();
                question.setHomeworkTemplate(template);
                question.setType(qRequest.getType());
                question.setQuestionText(qRequest.getQuestionText());
                question.setAttachedImageUrl(qRequest.getAttachedImageUrl());
                question.setAttachedAudioUrl(qRequest.getAttachedAudioUrl());
                question.setAttachedFileUrl(qRequest.getAttachedFileUrl());
                question.setSortOrder(qRequest.getSortOrder());

                if (qRequest.getType() == HomeworkQuestionType.QUIZ && qRequest.getOptions() != null) {
                    for (TeacherHomeworkQuestionOptionRequest oRequest : qRequest.getOptions()) {
                        HomeworkQuestionOption option = new HomeworkQuestionOption();
                        option.setQuestion(question);
                        option.setContent(oRequest.getContent());
                        option.setSortOrder(oRequest.getSortOrder());
                        option.setIsCorrect(oRequest.getIsCorrect());
                        question.getOptions().add(option);
                    }
                }

                if (qRequest.getType() == HomeworkQuestionType.ESSAY && qRequest.getRubric() != null) {
                    TeacherHomeworkRubricRequest rRequest = qRequest.getRubric();
                    HomeworkRubric rubric = new HomeworkRubric();
                    rubric.setQuestion(question);
                    rubric.setTitle(rRequest.getTitle());
                    rubric.setDescription(rRequest.getDescription());
                    
                    double rubricScore = 0.0;
                    if (rRequest.getCriteria() != null) {
                        for (TeacherHomeworkRubricCriterionRequest cRequest : rRequest.getCriteria()) {
                            HomeworkRubricCriterion criterion = new HomeworkRubricCriterion();
                            criterion.setRubric(rubric);
                            criterion.setName(cRequest.getName());
                            criterion.setDescription(cRequest.getDescription());
                            criterion.setMaxScore(cRequest.getMaxScore());
                            criterion.setDisplayOrder(cRequest.getDisplayOrder());
                            rubricScore += criterion.getMaxScore();
                            rubric.getCriteria().add(criterion);
                        }
                    }
                    rubric.setMaxScore(rubricScore);
                    question.setRubric(rubric);
                }
                
                question.setMaxScore(qRequest.getMaxScore()); 
                template.getQuestions().add(question);
            }
        }
        
        templateRepository.save(template);
    }

    @Transactional
    public void deleteHomeworkTemplate(Long templateId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();

        HomeworkTemplate template = templateRepository.findByIdAndCenter_Id(templateId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found."));

        if (!template.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Access denied.");
        }

        templateRepository.delete(template);
    }
    
    @Transactional
    public Long duplicateHomeworkTemplate(Long templateId, Long teacherId) {
        Long centerId = TenantContext.getCurrentTenantId();
        HomeworkTemplate existingTemplate = templateRepository.findWithDetailsByIdAndCenter_IdAndTeacher_Id(templateId, centerId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found or access denied."));
                
        HomeworkTemplate newTemplate = new HomeworkTemplate();
        newTemplate.setCenter(existingTemplate.getCenter());
        newTemplate.setTeacher(existingTemplate.getTeacher());
        newTemplate.setVersion(1);
        newTemplate.setTitle(existingTemplate.getTitle() + " (Copy)");
        newTemplate.setDescription(existingTemplate.getDescription());
        newTemplate.setInstructions(existingTemplate.getInstructions());
        newTemplate.setHomeworkType(existingTemplate.getHomeworkType());
        newTemplate.setEstimatedTime(existingTemplate.getEstimatedTime());
        newTemplate.setDifficulty(existingTemplate.getDifficulty());
        newTemplate.setMaxScore(existingTemplate.getMaxScore());
        
        for (HomeworkQuestion eq : existingTemplate.getQuestions()) {
            HomeworkQuestion nq = new HomeworkQuestion();
            nq.setHomeworkTemplate(newTemplate);
            nq.setType(eq.getType());
            nq.setQuestionText(eq.getQuestionText());
            nq.setAttachedImageUrl(eq.getAttachedImageUrl());
            nq.setAttachedAudioUrl(eq.getAttachedAudioUrl());
            nq.setAttachedFileUrl(eq.getAttachedFileUrl());
            nq.setSortOrder(eq.getSortOrder());
            nq.setMaxScore(eq.getMaxScore());
            
            if (eq.getOptions() != null) {
                for (HomeworkQuestionOption eo : eq.getOptions()) {
                    HomeworkQuestionOption no = new HomeworkQuestionOption();
                    no.setQuestion(nq);
                    no.setContent(eo.getContent());
                    no.setSortOrder(eo.getSortOrder());
                    no.setIsCorrect(eo.getIsCorrect());
                    nq.getOptions().add(no);
                }
            }
            
            if (eq.getRubric() != null) {
                HomeworkRubric er = eq.getRubric();
                HomeworkRubric nr = new HomeworkRubric();
                nr.setQuestion(nq);
                nr.setTitle(er.getTitle());
                nr.setDescription(er.getDescription());
                nr.setMaxScore(er.getMaxScore());
                
                if (er.getCriteria() != null) {
                    for (HomeworkRubricCriterion ec : er.getCriteria()) {
                        HomeworkRubricCriterion nc = new HomeworkRubricCriterion();
                        nc.setRubric(nr);
                        nc.setName(ec.getName());
                        nc.setDescription(ec.getDescription());
                        nc.setMaxScore(ec.getMaxScore());
                        nc.setDisplayOrder(ec.getDisplayOrder());
                        nr.getCriteria().add(nc);
                    }
                }
                nq.setRubric(nr);
            }
            newTemplate.getQuestions().add(nq);
        }
        
        HomeworkTemplate savedTemplate = templateRepository.save(newTemplate);
        return savedTemplate.getId();
    }
    
    @Transactional(readOnly = true)
    public java.util.List<com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkTemplateResponse> searchTemplates(Long teacherId, String keyword, com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type, com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty difficulty, Boolean archived) {
        Long centerId = TenantContext.getCurrentTenantId();
        
        return templateRepository.findAll().stream()
                .filter(t -> t.getCenter().getId().equals(centerId))
                .filter(t -> t.getTeacher().getId().equals(teacherId))
                .filter(t -> archived == null || t.getArchived().equals(archived))
                .filter(t -> type == null || t.getHomeworkType() == type)
                .filter(t -> difficulty == null || t.getDifficulty() == difficulty)
                .filter(t -> keyword == null || keyword.isEmpty() || t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkTemplateResponse mapToResponse(HomeworkTemplate template) {
        long assignmentCount = assignmentRepository.countByHomeworkTemplate_Id(template.getId());
        long activeAssignmentCount = assignmentCount; // We can refine this if Assignment has a status

        String status = "DRAFT";
        if (template.getArchived()) {
            status = "ARCHIVED";
        } else if (assignmentCount > 0) {
            status = "ACTIVE";
        }

        return com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkTemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .description(template.getDescription())
                .instructions(template.getInstructions())
                .homeworkType(template.getHomeworkType())
                .estimatedTime(template.getEstimatedTime())
                .difficulty(template.getDifficulty())
                .archived(template.getArchived())
                .version(template.getVersion())
                .parentTemplateId(template.getParentTemplateId())
                .maxScore(template.getMaxScore())
                .teacherId(template.getTeacher().getId())
                .teacherFullName(template.getTeacher().getFullName())
                .questions(template.getQuestions())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .assignmentCount(assignmentCount)
                .activeAssignmentCount(activeAssignmentCount)
                .status(status)
                .build();
    }
}
