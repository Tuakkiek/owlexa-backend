package com.owlexa.owlexabackend.modules.grading_criteria.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.grading_criteria.dto.request.GradingCriteriaRequest;
import com.owlexa.owlexabackend.modules.grading_criteria.dto.response.GradingCriteriaResponse;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class GradingCriteriaService {

    private final GradingCriteriaRepository gradingCriteriaRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final QuestionRepository questionRepository;
    private final RichTextDocumentService richTextDocumentService;
    private final FileReferenceService fileReferenceService;

    @Transactional(readOnly = true)
    public List<GradingCriteriaResponse> findAll(String search) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        List<GradingCriteria> criteria = search == null || search.isBlank()
                ? gradingCriteriaRepository.findAllByCenter_IdAndDeletedAtIsNullOrderByUpdatedAtDesc(centerId)
                : gradingCriteriaRepository.findAllByCenter_IdAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(
                        centerId,
                        search.trim()
                );

        return criteria.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GradingCriteriaResponse findById(Long criteriaId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return toResponse(findActiveCriteria(criteriaId, centerId));
    }

    @Transactional
    public GradingCriteriaResponse create(GradingCriteriaRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        JsonNode content = requireContent(request.getContent());

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        GradingCriteria criteria = GradingCriteria.builder()
                .center(center)
                .name(request.getName().trim())
                .contentJson(richTextDocumentService.serialize(content))
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        GradingCriteria saved = gradingCriteriaRepository.save(criteria);
        fileReferenceService.syncDocumentReferences(FileOwnerType.GRADING_CRITERIA, saved.getId(), centerId, content);
        return toResponse(saved);
    }

    @Transactional
    public GradingCriteriaResponse update(Long criteriaId, GradingCriteriaRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        JsonNode content = requireContent(request.getContent());

        GradingCriteria criteria = findActiveCriteria(criteriaId, centerId);
        criteria.setName(request.getName().trim());
        criteria.setContentJson(richTextDocumentService.serialize(content));
        criteria.setUpdatedBy(currentUser);

        GradingCriteria saved = gradingCriteriaRepository.save(criteria);
        fileReferenceService.syncDocumentReferences(FileOwnerType.GRADING_CRITERIA, saved.getId(), centerId, content);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long criteriaId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        GradingCriteria criteria = findActiveCriteria(criteriaId, centerId);
        validateDelete(criteria);
        criteria.setDeletedAt(Instant.now());
        criteria.setUpdatedBy(currentUser);
        gradingCriteriaRepository.save(criteria);
        fileReferenceService.syncReferences(
                FileOwnerType.GRADING_CRITERIA,
                criteria.getId(),
                centerId,
                List.of()
        );
    }

    private void validateDelete(GradingCriteria criteria) {
        boolean isUsedByActiveEssayQuestion = questionRepository
                .existsByGradingCriteria_IdAndCenter_IdAndTypeAndDeletedAtIsNull(
                        criteria.getId(),
                        criteria.getCenterId(),
                        QuestionType.ESSAY
                );

        if (isUsedByActiveEssayQuestion) {
            throw new BusinessRuleException(
                    "GRADING_CRITERIA_IN_USE",
                    "Không thể xóa tiêu chí chấm vì đang được sử dụng bởi câu hỏi tự luận"
            );
        }
    }

    private JsonNode requireContent(JsonNode content) {
        JsonNode normalized = richTextDocumentService.normalize(content);
        if (!richTextDocumentService.hasMeaningfulContent(normalized)) {
            throw new BadRequestException("Nội dung tiêu chí chấm không được để trống");
        }
        return normalized;
    }

    private GradingCriteria findActiveCriteria(Long criteriaId, Long centerId) {
        return gradingCriteriaRepository.findByIdAndCenter_IdAndDeletedAtIsNull(criteriaId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Grading criteria not found with id: " + criteriaId));
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage grading criteria");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }

        return currentUser;
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        return centerId;
    }

    private GradingCriteriaResponse toResponse(GradingCriteria criteria) {
        return GradingCriteriaResponse.builder()
                .id(criteria.getId())
                .name(criteria.getName())
                .content(richTextDocumentService.deserialize(criteria.getContentJson()))
                .createdAt(criteria.getCreatedAt())
                .updatedAt(criteria.getUpdatedAt())
                .build();
    }
}
