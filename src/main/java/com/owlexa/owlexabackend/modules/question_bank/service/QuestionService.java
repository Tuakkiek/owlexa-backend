package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionOptionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.mapper.QuestionMapper;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionSpecifications;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?:nbsp|#160);", Pattern.CASE_INSENSITIVE);
    private static final String QUESTION_CODE_PREFIX = "Q-";
    private static final String TEMPORARY_QUESTION_CODE_PREFIX = "TMP-";
    private static final int QUESTION_CODE_MINIMUM_DIGITS = 6;
    private static final int TEMPORARY_QUESTION_CODE_UUID_LENGTH = 28;
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("displayOrder", "createdAt", "updatedAt");

    private final QuestionRepository questionRepository;
    private final QuestionCollectionRepository collectionRepository;
    private final GradingCriteriaRepository gradingCriteriaRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final RichTextDocumentService richTextDocumentService;
    private final FileReferenceService fileReferenceService;
    private final QuestionMapper questionMapper;

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findAll(
            String search,
            Long collectionId,
            String sectionCode,
            QuestionType type,
            QuestionDifficulty difficulty,
            Long gradingCriteriaId,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateSort(pageable);
        Pageable effectivePageable = applyDefaultSort(pageable, collectionId);
        String normalizedSectionCode = sectionCode == null || sectionCode.isBlank()
                ? null
                : normalizeSectionCode(sectionCode);

        return questionRepository.findAll(
                        QuestionSpecifications.search(
                                centerId,
                                search,
                                collectionId,
                                normalizedSectionCode,
                                type,
                                difficulty,
                                gradingCriteriaId
                        ),
                        effectivePageable
                )
                .map(questionMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(Long questionId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return questionMapper.toDetailResponse(findActiveQuestion(questionId, centerId));
    }

    @Transactional(readOnly = true)
    public List<String> findSectionCodes(Long collectionId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        QuestionCollection collection = resolveActiveCollection(collectionId, centerId);
        return questionRepository.findActiveSectionCodes(collection.getId());
    }

    @Transactional(readOnly = true)
    public void validateImportBatch(List<QuestionRequest> requests) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        Set<String> requestedOrders = new HashSet<>();

        for (QuestionRequest request : requests) {
            validateQuestionRequest(request);
            request.setSectionCode(normalizeSectionCode(request.getSectionCode()));
            QuestionCollection collection = resolveActiveCollection(request.getCollectionId(), centerId);
            String orderKey = collection.getId() + ":" + request.getDisplayOrder();
            if (!requestedOrders.add(orderKey)) {
                throw new DuplicateResourceException(
                        "Import contains duplicate display order "
                                + request.getDisplayOrder()
                                + " in collection "
                                + collection.getCode()
                );
            }
            validateDisplayOrderAvailable(collection.getId(), request.getDisplayOrder(), null);
        }
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateQuestionRequest(request);
        QuestionCollection collection = resolveActiveCollection(request.getCollectionId(), centerId);
        String sectionCode = normalizeSectionCode(request.getSectionCode());
        validateDisplayOrderAvailable(collection.getId(), request.getDisplayOrder(), null);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));
        GradingCriteria gradingCriteria = resolveGradingCriteria(request, centerId);
        JsonNode content = richTextDocumentService.normalize(request.getContent());
        JsonNode explanation = richTextDocumentService.normalizeOptional(request.getExplanation());
        JsonNode sampleAnswer = richTextDocumentService.normalizeOptional(request.getSampleAnswer());

        Question question = Question.builder()
                .center(center)
                .collection(collection)
                .sectionCode(sectionCode)
                .displayOrder(request.getDisplayOrder())
                .type(request.getType())
                .contentJson(richTextDocumentService.serialize(content))
                .difficulty(request.getDifficulty())
                .points(request.getPoints())
                .gradingCriteria(gradingCriteria)
                .explanationJson(richTextDocumentService.serializeOptional(explanation))
                .sampleAnswerJson(richTextDocumentService.serializeOptional(sampleAnswer))
                .questionCode(generateTemporaryQuestionCode())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        replaceOptions(question, request.getOptions());

        Question saved = questionRepository.saveAndFlush(question);
        saved.setQuestionCode(formatQuestionCode(saved.getId()));
        fileReferenceService.syncReferences(
                FileOwnerType.QUESTION,
                saved.getId(),
                centerId,
                java.util.stream.Stream.of(content, explanation, sampleAnswer)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );
        return questionMapper.toDetailResponse(saved);
    }

    @Transactional
    public QuestionResponse update(Long questionId, QuestionRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateQuestionRequest(request);

        Question question = findActiveQuestion(questionId, centerId);
        QuestionCollection collection = resolveActiveCollection(request.getCollectionId(), centerId);
        String sectionCode = normalizeSectionCode(request.getSectionCode());
        validateDisplayOrderAvailable(collection.getId(), request.getDisplayOrder(), questionId);
        GradingCriteria gradingCriteria = resolveGradingCriteria(request, centerId);
        JsonNode content = richTextDocumentService.normalize(request.getContent());
        JsonNode explanation = richTextDocumentService.normalizeOptional(request.getExplanation());
        JsonNode sampleAnswer = richTextDocumentService.normalizeOptional(request.getSampleAnswer());

        question.setCollection(collection);
        question.setSectionCode(sectionCode);
        question.setDisplayOrder(request.getDisplayOrder());
        question.setType(request.getType());
        question.setContentJson(richTextDocumentService.serialize(content));
        question.setDifficulty(request.getDifficulty());
        question.setPoints(request.getPoints());
        question.setGradingCriteria(gradingCriteria);
        question.setExplanationJson(richTextDocumentService.serializeOptional(explanation));
        question.setSampleAnswerJson(richTextDocumentService.serializeOptional(sampleAnswer));
        question.setUpdatedBy(currentUser);

        replaceOptions(question, request.getOptions());

        Question saved = questionRepository.saveAndFlush(question);
        fileReferenceService.syncReferences(
                FileOwnerType.QUESTION,
                saved.getId(),
                centerId,
                java.util.stream.Stream.of(content, explanation, sampleAnswer)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );
        return questionMapper.toDetailResponse(saved);
    }

    @Transactional
    public void delete(Long questionId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Question question = findActiveQuestion(questionId, centerId);
        question.setDeletedAt(Instant.now());
        question.setUpdatedBy(currentUser);
        questionRepository.save(question);
        fileReferenceService.syncReferences(
                FileOwnerType.QUESTION,
                question.getId(),
                centerId,
            List.of()
        );
    }

    @Transactional
    public void deleteMany(List<Long> questionIds) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BadRequestException("Question ids are required");
        }

        List<Long> distinctIds = questionIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            throw new BadRequestException("Question ids are required");
        }

        for (Long questionId : distinctIds) {
            Question question = findActiveQuestion(questionId, centerId);
            question.setDeletedAt(Instant.now());
            question.setUpdatedBy(currentUser);
            questionRepository.save(question);
            fileReferenceService.syncReferences(
                    FileOwnerType.QUESTION,
                    question.getId(),
                    centerId,
                    List.of()
            );
        }
    }

    private void validateQuestionRequest(QuestionRequest request) {
        if (request.getCollectionId() == null) {
            throw new BadRequestException("Collection id is required");
        }
        normalizeSectionCode(request.getSectionCode());
        if (request.getDisplayOrder() == null || request.getDisplayOrder() < 1) {
            throw new BadRequestException("Display order must be greater than or equal to 1");
        }
        if (request.getType() == null) {
            throw new BadRequestException("Loại câu hỏi không được để trống");
        }
        validatePoints(request);

        if (request.getType() == QuestionType.MULTIPLE_CHOICE) {
            validateMultipleChoice(request);
            return;
        }

        if (request.getType() == QuestionType.ESSAY) {
            validateEditorContent(request.getContent(), "Nội dung câu hỏi không được để trống");
            validateEssay(request);
        }
    }

    private void validateMultipleChoice(QuestionRequest request) {
        if (request.getGradingCriteriaId() != null) {
            throw new BadRequestException("Câu hỏi trắc nghiệm không được gắn tiêu chí chấm");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new BadRequestException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
        }
        if (request.getOptions().stream().anyMatch(option -> option == null)) {
            throw new BadRequestException("Lựa chọn đáp án không hợp lệ");
        }

        request.getOptions().forEach(this::validateOptionFields);

        long correctCount = request.getOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .count();
        if (correctCount == 0) {
            throw new BadRequestException("Câu hỏi trắc nghiệm phải có ít nhất 1 đáp án đúng");
        }

        request.getOptions().forEach(option ->
                validatePlainText(option.getContent(), "Nội dung lựa chọn không được để trống")
        );
    }

    private void validatePoints(QuestionRequest request) {
        if (request.getPoints() != null && request.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Điểm câu hỏi phải lớn hơn 0");
        }
    }

    private void validateOptionFields(QuestionOptionRequest option) {
        if (option.getIsCorrect() == null) {
            throw new BadRequestException("Trạng thái đáp án đúng không được để trống");
        }
        if (option.getDisplayOrder() == null || option.getDisplayOrder() < 1) {
            throw new BadRequestException("Thứ tự lựa chọn phải lớn hơn hoặc bằng 1");
        }
    }

    private void validateEssay(QuestionRequest request) {
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            throw new BadRequestException("Câu hỏi tự luận không được có lựa chọn đáp án");
        }
    }

    private GradingCriteria resolveGradingCriteria(QuestionRequest request, Long centerId) {
        if (request.getGradingCriteriaId() == null) {
            return null;
        }

        if (request.getType() != QuestionType.ESSAY) {
            throw new BadRequestException("Chỉ câu hỏi tự luận mới được gắn tiêu chí chấm");
        }

        return gradingCriteriaRepository
                .findByIdAndCenter_IdAndDeletedAtIsNull(request.getGradingCriteriaId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grading criteria not found with id: " + request.getGradingCriteriaId()
                ));
    }

    private void replaceOptions(Question question, List<QuestionOptionRequest> optionRequests) {
        question.getOptions().clear();

        if (question.getType() != QuestionType.MULTIPLE_CHOICE || optionRequests == null) {
            return;
        }

        optionRequests.stream()
                .sorted(Comparator.comparing(QuestionOptionRequest::getDisplayOrder))
                .map(optionRequest -> QuestionOption.builder()
                        .question(question)
                        .content(optionRequest.getContent().trim())
                        .isCorrect(optionRequest.getIsCorrect())
                        .displayOrder(optionRequest.getDisplayOrder())
                        .build())
                .forEach(question.getOptions()::add);
    }

    private Question findActiveQuestion(Long questionId, Long centerId) {
        return questionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(questionId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
    }

    private QuestionCollection resolveActiveCollection(Long collectionId, Long centerId) {
        return collectionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(collectionId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question collection not found with id: " + collectionId
                ));
    }

    private void validateDisplayOrderAvailable(
            Long collectionId,
            Integer displayOrder,
            Long excludedQuestionId
    ) {
        boolean exists = excludedQuestionId == null
                ? questionRepository.existsByCollection_IdAndDisplayOrderAndDeletedAtIsNull(
                        collectionId,
                        displayOrder
                )
                : questionRepository.existsByCollection_IdAndDisplayOrderAndDeletedAtIsNullAndIdNot(
                        collectionId,
                        displayOrder,
                        excludedQuestionId
                );
        if (exists) {
            throw new DuplicateResourceException(
                    "Display order " + displayOrder + " already exists in collection " + collectionId
            );
        }
    }

    private String normalizeSectionCode(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Section code is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SECTION_CODE_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(
                    "Section code must match ^[A-Z][A-Z0-9_]{0,49}$"
            );
        }
        return normalized;
    }

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported question sort: " + order.getProperty());
            }
        });
    }

    private Pageable applyDefaultSort(Pageable pageable, Long collectionId) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        Sort defaultSort = collectionId == null
                ? Sort.by(Sort.Order.desc("updatedAt"))
                : Sort.by(Sort.Order.asc("displayOrder"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage questions");
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

    private void validatePlainText(String content, String message) {
        if (content == null) {
            throw new BadRequestException(message);
        }
        String normalized = HTML_ENTITY_PATTERN.matcher(content).replaceAll(" ");
        String textOnly = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
        if (textOnly.trim().isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private void validateEditorContent(JsonNode content, String message) {
        JsonNode normalized = richTextDocumentService.normalize(content);
        if (!richTextDocumentService.hasMeaningfulContent(normalized)) {
            throw new BadRequestException(message);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String generateTemporaryQuestionCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return TEMPORARY_QUESTION_CODE_PREFIX + uuid.substring(0, TEMPORARY_QUESTION_CODE_UUID_LENGTH);
    }

    private String formatQuestionCode(Long questionId) {
        return QUESTION_CODE_PREFIX + String.format("%0" + QUESTION_CODE_MINIMUM_DIGITS + "d", questionId);
    }

}
