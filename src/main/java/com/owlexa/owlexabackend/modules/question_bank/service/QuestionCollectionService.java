package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionCreateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionUpdateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.mapper.QuestionCollectionMapper;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionCollectionService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

    private final QuestionCollectionRepository collectionRepository;
    private final QuestionRepository questionRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final QuestionCollectionMapper collectionMapper;

    @Transactional(readOnly = true)
    public List<QuestionCollectionResponse> findAll() {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        List<QuestionCollection> collections =
                collectionRepository.findAllByCenter_IdAndCreatedBy_IdAndDeletedAtIsNullOrderByNameAsc(centerId, currentUser.getId());
        if (collections.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> counts = questionRepository.countActiveByCollectionIds(
                        collections.stream().map(QuestionCollection::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        QuestionRepository.CollectionQuestionCount::getCollectionId,
                        QuestionRepository.CollectionQuestionCount::getQuestionCount
                ));

        return collections.stream()
                .map(collection -> collectionMapper.toResponse(
                        collection,
                        counts.getOrDefault(collection.getId(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionCollectionResponse findById(Long collectionId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        QuestionCollection collection = findActiveCollection(collectionId, centerId);
        return collectionMapper.toResponse(
                collection,
                questionRepository.countByCollection_IdAndDeletedAtIsNull(collectionId)
        );
    }

    @Transactional
    public QuestionCollectionResponse create(QuestionCollectionCreateRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        String code = normalizeAndValidateCode(request.getCode());
        String name = normalizeRequiredName(request.getName());
        String description = normalizeOptionalText(request.getDescription());

        if (collectionRepository.existsByCenter_IdAndCreatedBy_IdAndCode(centerId, currentUser.getId(), code)) {
            throw new DuplicateResourceException("Collection code already exists: " + code);
        }
        validateActiveNameAvailable(centerId, currentUser.getId(), name, null);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));
        QuestionCollection collection = QuestionCollection.builder()
                .center(center)
                .code(code)
                .name(name)
                .description(description)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        QuestionCollection saved = collectionRepository.saveAndFlush(collection);
        return collectionMapper.toResponse(saved, 0L);
    }

    @Transactional
    public QuestionCollectionResponse update(
            Long collectionId,
            QuestionCollectionUpdateRequest request
    ) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        QuestionCollection collection = findActiveCollection(collectionId, centerId);
        String name = normalizeRequiredName(request.getName());
        validateActiveNameAvailable(centerId, currentUser.getId(), name, collectionId);

        collection.setName(name);
        collection.setDescription(normalizeOptionalText(request.getDescription()));
        collection.setUpdatedBy(currentUser);

        QuestionCollection saved = collectionRepository.saveAndFlush(collection);
        return collectionMapper.toResponse(
                saved,
                questionRepository.countByCollection_IdAndDeletedAtIsNull(collectionId)
        );
    }

    @Transactional
    public void delete(Long collectionId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        QuestionCollection collection = findActiveCollection(collectionId, centerId);

        if (questionRepository.existsByCollection_IdAndDeletedAtIsNull(collectionId)) {
            throw new BusinessRuleException(
                    "QUESTION_COLLECTION_IN_USE",
                    "Collection cannot be deleted while it contains active questions"
            );
        }

        collection.setDeletedAt(Instant.now());
        collection.setUpdatedBy(currentUser);
        collectionRepository.save(collection);
    }

    @Transactional(readOnly = true)
    public QuestionCollection requireActiveByCode(String rawCode) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        String code = normalizeAndValidateCode(rawCode);
        return collectionRepository.findByCodeAndCenter_IdAndCreatedBy_IdAndDeletedAtIsNull(code, centerId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question collection not found with code: " + code
                ));
    }

    @Transactional(readOnly = true)
    public QuestionCollection requireActiveById(Long collectionId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        if (collectionId == null) {
            throw new BadRequestException("Collection id is required");
        }
        return findActiveCollection(collectionId, centerId);
    }

    private QuestionCollection findActiveCollection(Long collectionId, Long centerId) {
        User currentUser = authorizationService.getCurrentUser();
        return collectionRepository.findByIdAndCenter_IdAndCreatedBy_IdAndDeletedAtIsNull(collectionId, centerId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question collection not found with id: " + collectionId
                ));
    }

    private void validateActiveNameAvailable(Long centerId, Long createdById, String name, Long excludedId) {
        boolean exists = excludedId == null
                ? collectionRepository.existsByCenter_IdAndCreatedBy_IdAndNameIgnoreCaseAndDeletedAtIsNull(centerId, createdById, name)
                : collectionRepository.existsByCenter_IdAndCreatedBy_IdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
                        centerId,
                        createdById,
                        name,
                        excludedId
                );
        if (exists) {
            throw new DuplicateResourceException("Active collection name already exists: " + name);
        }
    }

    private String normalizeAndValidateCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Collection code is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(
                    "Collection code must match ^[A-Z][A-Z0-9_]{0,63}$"
            );
        }
        return normalized;
    }

    private String normalizeRequiredName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Collection name is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 255) {
            throw new BadRequestException("Collection name must not exceed 255 characters");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 1000) {
            throw new BadRequestException("Collection description must not exceed 1000 characters");
        }
        return normalized;
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage question collections");
        }
        if (!membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId)) {
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
}
