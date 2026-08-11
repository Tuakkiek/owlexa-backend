package com.owlexa.owlexabackend.modules.assessment_builder.service;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentItemRequest;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentRequest;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentDetailResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentListResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItemOption;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.assessment_builder.mapper.AssessmentMapper;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentRepository;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentSpecifications;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.entity.FileType;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?:nbsp|#160);", Pattern.CASE_INSENSITIVE);

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final StoredFileRepository storedFileRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final AssessmentMapper assessmentMapper;
    private final RichTextDocumentService richTextDocumentService;
    private final FileReferenceService fileReferenceService;

    @Transactional(readOnly = true)
    public Page<AssessmentListResponse> findAll(
            String search,
            AssessmentStatus status,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assessmentRepository.findAll(
                        AssessmentSpecifications.search(centerId, search, status),
                        pageable
                )
                .map(assessmentMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public AssessmentDetailResponse findById(Long assessmentId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assessmentMapper.toDetailResponse(findActiveAssessment(assessmentId, centerId));
    }

    @Transactional
    public AssessmentDetailResponse create(AssessmentRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateAssessmentRequest(request);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        Assessment assessment = Assessment.builder()
                .center(center)
                .status(AssessmentStatus.DRAFT)
                .title(request.getTitle().trim())
                .description(normalizeOptionalText(request.getDescription()))
                .audioFile(resolveAudioFile(request.getAudioFileId(), centerId))
                .playbackMode(resolvePlaybackMode(request.getPlaybackMode()))
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        JsonNode content = richTextDocumentService.normalize(request.getContent(), request.getDescription());
        assessment.setContentJson(richTextDocumentService.serialize(content));
        assessment.setDescription(toDescriptionSummary(content));

        replaceBlocksAndItems(assessment, request.getBlocks(), request.getItems(), request.getContent(), centerId);

        Assessment saved = assessmentRepository.save(assessment);
        fileReferenceService.syncReferences(
                FileOwnerType.ASSESSMENT,
                saved.getId(),
                centerId,
                referenceDocuments(saved, content),
                referencedFileIds(saved)
        );
        return assessmentMapper.toDetailResponse(saved);
    }

    @Transactional
    public AssessmentDetailResponse update(Long assessmentId, AssessmentRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateAssessmentRequest(request);

        Assessment assessment = findActiveAssessment(assessmentId, centerId);

        assessment.setTitle(request.getTitle().trim());
        assessment.setAudioFile(resolveAudioFile(request.getAudioFileId(), centerId));
        assessment.setPlaybackMode(resolvePlaybackMode(request.getPlaybackMode()));
        JsonNode content = richTextDocumentService.normalize(request.getContent(), request.getDescription());
        assessment.setContentJson(richTextDocumentService.serialize(content));
        assessment.setDescription(toDescriptionSummary(content));
        assessment.setUpdatedBy(currentUser);

        replaceBlocksAndItems(assessment, request.getBlocks(), request.getItems(), request.getContent(), centerId);

        Assessment saved = assessmentRepository.save(assessment);
        fileReferenceService.syncReferences(
                FileOwnerType.ASSESSMENT,
                saved.getId(),
                centerId,
                referenceDocuments(saved, content),
                referencedFileIds(saved)
        );
        return assessmentMapper.toDetailResponse(saved);
    }

    @Transactional
    public AssessmentDetailResponse publish(Long assessmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assessment assessment = findActiveAssessment(assessmentId, centerId);

        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new BadRequestException("Only draft assessments can be published");
        }

        validatePublishable(assessment);
        assessment.setStatus(AssessmentStatus.PUBLISHED);
        assessment.setUpdatedBy(currentUser);

        return assessmentMapper.toDetailResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentDetailResponse archive(Long assessmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assessment assessment = findActiveAssessment(assessmentId, centerId);
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new BadRequestException("Only published assessments can be archived");
        }

        assessment.setStatus(AssessmentStatus.ARCHIVED);
        assessment.setUpdatedBy(currentUser);

        return assessmentMapper.toDetailResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public void delete(Long assessmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assessment assessment = findActiveAssessment(assessmentId, centerId);
        assessment.setDeletedAt(Instant.now());
        assessment.setUpdatedBy(currentUser);
        assessmentRepository.save(assessment);
        fileReferenceService.syncReferences(
                FileOwnerType.ASSESSMENT,
                assessment.getId(),
                centerId,
                List.of()
        );
    }

    private List<JsonNode> referenceDocuments(Assessment assessment, JsonNode assessmentContent) {
        List<JsonNode> documents = new ArrayList<>();
        documents.add(assessmentContent);
        for (AssessmentItem item : assessment.getItems()) {
            documents.add(richTextDocumentService.deserialize(item.getContentJson()));
            addOptionalDocument(documents, item.getExplanationJson());
            addOptionalDocument(documents, item.getSampleAnswerJson());
            addOptionalDocument(documents, item.getGradingCriteriaContentJson());
        }
        return documents;
    }

    private void addOptionalDocument(List<JsonNode> documents, String serialized) {
        JsonNode document = richTextDocumentService.deserializeOptional(serialized);
        if (document != null) {
            documents.add(document);
        }
    }

    private void validateAssessmentRequest(AssessmentRequest request) {
        validateContentHasText(request.getTitle(), "Assessment title is required");
        validateItems(request.getItems());
    }

    private void validateItems(List<AssessmentItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<Long> questionIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();

        for (AssessmentItemRequest item : items) {
            if (item == null) {
                throw new BadRequestException("Assessment item is invalid");
            }
            if (item.getQuestionId() == null) {
                throw new BadRequestException("Question id is required");
            }
            if (!questionIds.add(item.getQuestionId())) {
                throw new BadRequestException("Assessment cannot contain duplicate questions");
            }
            if (item.getDisplayOrder() == null || item.getDisplayOrder() < 1) {
                throw new BadRequestException("Display order must be greater than or equal to 1");
            }
            if (!displayOrders.add(item.getDisplayOrder())) {
                throw new BadRequestException("Assessment item display order must be unique");
            }
            if (item.getPoints() != null && item.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Item points must be greater than 0");
            }
        }
    }

    private void replaceBlocksAndItems(
            Assessment assessment,
            List<com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentBlockRequest> blockRequests,
            List<AssessmentItemRequest> itemRequests,
            JsonNode fallbackContent,
            Long centerId
    ) {
        assessment.getBlocks().clear();
        assessment.getItems().clear();

        List<com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock> blocks = new ArrayList<>();
        if (blockRequests != null && !blockRequests.isEmpty()) {
            int pos = 0;
            for (com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentBlockRequest req : blockRequests) {
                JsonNode normalized = richTextDocumentService.normalize(req.getContent(), null);
                com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock block =
                        com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock.builder()
                                .assessment(assessment)
                                .position(req.getPosition() != null ? req.getPosition() : pos++)
                                .title(req.getTitle() != null ? req.getTitle().trim() : null)
                                .contentJson(richTextDocumentService.serialize(normalized))
                                .build();
                blocks.add(block);
            }
        } else {
            JsonNode normalized = richTextDocumentService.normalize(fallbackContent, null);
            com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock block =
                    com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock.builder()
                            .assessment(assessment)
                            .position(0)
                            .title("Nội dung chính")
                            .contentJson(richTextDocumentService.serialize(normalized))
                            .build();
            blocks.add(block);
        }
        assessment.getBlocks().addAll(blocks);

        Set<Long> seenQuestionIds = new HashSet<>();
        int currentDisplayOrder = 1;

        for (com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock block : blocks) {
            JsonNode blockDoc = richTextDocumentService.deserialize(block.getContentJson());
            List<ExtractedQuestionNode> extracted = new ArrayList<>();
            extractQuestionNodes(blockDoc, extracted);

            for (ExtractedQuestionNode eq : extracted) {
                if (!seenQuestionIds.add(eq.questionId())) {
                    throw new BadRequestException("Assessment cannot contain duplicate questions (Question ID: " + eq.questionId() + ")");
                }
                Question question = questionRepository
                        .findByIdAndCenter_IdAndDeletedAtIsNull(eq.questionId(), centerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + eq.questionId()));

                validateQuestionSnapshotSource(question, assessment.getAudioFile() != null);
                BigDecimal points = eq.points() != null ? eq.points() : question.getPoints();
                AssessmentItem item = assessmentMapper.toItemSnapshot(question, points, currentDisplayOrder++);
                item.setAssessment(assessment);
                item.setBlock(block);
                assessment.getItems().add(item);
            }
        }

        if (assessment.getItems().isEmpty() && itemRequests != null && !itemRequests.isEmpty()) {
            com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock firstBlock = blocks.get(0);
            for (AssessmentItemRequest itemRequest : itemRequests) {
                Question question = questionRepository
                        .findByIdAndCenter_IdAndDeletedAtIsNull(itemRequest.getQuestionId(), centerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + itemRequest.getQuestionId()));
                validateQuestionSnapshotSource(question, assessment.getAudioFile() != null);
                BigDecimal points = itemRequest.getPoints() != null ? itemRequest.getPoints() : question.getPoints();
                AssessmentItem item = assessmentMapper.toItemSnapshot(question, points, itemRequest.getDisplayOrder());
                item.setAssessment(assessment);
                item.setBlock(firstBlock);
                assessment.getItems().add(item);
            }
        }
    }

    private void extractQuestionNodes(JsonNode node, List<ExtractedQuestionNode> extracted) {
        if (node == null || !node.isObject()) return;
        String type = node.path("type").asText(null);
        if ("assessmentQuestion".equals(type)) {
            JsonNode attrs = node.path("attrs");
            if (attrs.has("questionId") && attrs.get("questionId").isNumber()) {
                Long qId = attrs.get("questionId").asLong();
                BigDecimal points = attrs.has("points") && attrs.get("points").isNumber()
                        ? BigDecimal.valueOf(attrs.get("points").asDouble())
                        : null;
                extracted.add(new ExtractedQuestionNode(qId, points));
            }
        }
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                extractQuestionNodes(child, extracted);
            }
        }
    }

    private record ExtractedQuestionNode(Long questionId, BigDecimal points) {}

    private AssessmentItem toAssessmentItem(
            Assessment assessment,
            AssessmentItemRequest itemRequest,
            Long centerId
    ) {
        Question question = questionRepository
                .findByIdAndCenter_IdAndDeletedAtIsNull(itemRequest.getQuestionId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found with id: " + itemRequest.getQuestionId()
                ));

        validateQuestionSnapshotSource(question, assessment.getAudioFile() != null);

        BigDecimal points = itemRequest.getPoints() != null ? itemRequest.getPoints() : question.getPoints();
        AssessmentItem item = assessmentMapper.toItemSnapshot(question, points, itemRequest.getDisplayOrder());
        item.setAssessment(assessment);
        return item;
    }

    private void validateQuestionSnapshotSource(Question question, boolean assessmentHasAudio) {
        if (question.getType() == QuestionType.MULTIPLE_CHOICE) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                throw new BadRequestException("Multiple choice question must have at least 2 options");
            }

            long correctCount = question.getOptions().stream()
                    .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                    .count();
            if (correctCount == 0) {
                throw new BadRequestException("Multiple choice question must have at least 1 correct option");
            }
        }

        validateQuestionRenderable(
                question.getType(),
                richTextDocumentService.deserialize(question.getContentJson()),
                assessmentHasAudio
        );
    }

    private void validatePublishable(Assessment assessment) {
        if (assessment.getItems() == null || assessment.getItems().isEmpty()) {
            throw new BadRequestException("Assessment must contain at least 1 question before publishing");
        }

        boolean assessmentHasAudio = assessment.getAudioFile() != null;
        for (AssessmentItem item : assessment.getItems()) {
            validateQuestionRenderable(
                    item.getQuestionType(),
                    richTextDocumentService.deserialize(item.getContentJson()),
                    assessmentHasAudio
            );
            if (item.getDisplayOrder() == null || item.getDisplayOrder() < 1) {
                throw new BadRequestException("Assessment item display order is invalid");
            }
            if (item.getPoints() != null && item.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Assessment item points must be greater than 0");
            }
            if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                validateMultipleChoiceSnapshot(item);
            }
        }
    }

    private void validateQuestionRenderable(QuestionType questionType, JsonNode content, boolean assessmentHasAudio) {
        if (richTextDocumentService.hasMeaningfulContent(content)) {
            return;
        }
        if (questionType == QuestionType.MULTIPLE_CHOICE) {
            return;
        }
        throw new BadRequestException("Assessment item content is invalid");
    }

    private void validateMultipleChoiceSnapshot(AssessmentItem item) {
        List<AssessmentItemOption> options = item.getOptions();
        if (options == null || options.size() < 2) {
            throw new BadRequestException("Multiple choice assessment item must have at least 2 options");
        }

        long correctCount = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .count();
        if (correctCount == 0) {
            throw new BadRequestException("Multiple choice assessment item must have at least 1 correct option");
        }
    }

    private Assessment findActiveAssessment(Long assessmentId, Long centerId) {
        return assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(assessmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + assessmentId));
    }



    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage assessments");
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

    private void validateContentHasText(String content, String message) {
        if (content == null) {
            throw new BadRequestException(message);
        }
        String normalized = HTML_ENTITY_PATTERN.matcher(content).replaceAll(" ");
        String textOnly = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
        if (textOnly.trim().isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private StoredFile resolveAudioFile(Long audioFileId, Long centerId) {
        if (audioFileId == null) {
            return null;
        }
        StoredFile audioFile = storedFileRepository.findByIdAndCenter_IdAndDeletedAtIsNull(audioFileId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Audio file not found with id: " + audioFileId));
        if (audioFile.getFileType() != FileType.AUDIO) {
            throw new BadRequestException("Assessment audio file must be an audio file");
        }
        return audioFile;
    }

    private PlaybackMode resolvePlaybackMode(PlaybackMode playbackMode) {
        return playbackMode == null ? PlaybackMode.PRACTICE : playbackMode;
    }

    private List<Long> referencedFileIds(Assessment assessment) {
        StoredFile audioFile = assessment.getAudioFile();
        return audioFile == null ? List.of() : List.of(audioFile.getId());
    }

    private String toDescriptionSummary(JsonNode content) {
        String plainText = richTextDocumentService.toPlainText(content);
        if (plainText.isBlank()) {
            return null;
        }
        return plainText.length() <= 500 ? plainText : plainText.substring(0, 497) + "...";
    }
}
