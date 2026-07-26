package com.owlexa.owlexabackend.modules.assessment_builder.service;

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
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.mapper.AssessmentMapper;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentRepository;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentSpecifications;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?:nbsp|#160);", Pattern.CASE_INSENSITIVE);

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final AssessmentMapper assessmentMapper;

    @Transactional(readOnly = true)
    public Page<AssessmentListResponse> findAll(
            String search,
            AssessmentType type,
            AssessmentStatus status,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assessmentRepository.findAll(
                        AssessmentSpecifications.search(centerId, search, type, status),
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
                .type(request.getType())
                .status(AssessmentStatus.DRAFT)
                .title(request.getTitle().trim())
                .description(normalizeOptionalText(request.getDescription()))
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        replaceItems(assessment, request.getItems(), centerId);

        return assessmentMapper.toDetailResponse(assessmentRepository.save(assessment));
    }

    @Transactional
    public AssessmentDetailResponse update(Long assessmentId, AssessmentRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateAssessmentRequest(request);

        Assessment assessment = findActiveAssessment(assessmentId, centerId);
        assessment.setType(request.getType());
        assessment.setTitle(request.getTitle().trim());
        assessment.setDescription(normalizeOptionalText(request.getDescription()));
        assessment.setUpdatedBy(currentUser);

        replaceItems(assessment, request.getItems(), centerId);

        return assessmentMapper.toDetailResponse(assessmentRepository.save(assessment));
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
    }

    private void validateAssessmentRequest(AssessmentRequest request) {
        if (request.getType() == null) {
            throw new BadRequestException("Assessment type is required");
        }
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

    private void replaceItems(Assessment assessment, List<AssessmentItemRequest> itemRequests, Long centerId) {
        assessment.getItems().clear();

        if (itemRequests == null || itemRequests.isEmpty()) {
            return;
        }

        itemRequests.stream()
                .sorted(Comparator.comparing(AssessmentItemRequest::getDisplayOrder))
                .map(itemRequest -> toAssessmentItem(assessment, itemRequest, centerId))
                .forEach(assessment.getItems()::add);
    }

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

        validateQuestionSnapshotSource(question);

        BigDecimal points = itemRequest.getPoints() != null ? itemRequest.getPoints() : question.getPoints();
        AssessmentItem item = assessmentMapper.toItemSnapshot(question, points, itemRequest.getDisplayOrder());
        item.setAssessment(assessment);
        return item;
    }

    private void validateQuestionSnapshotSource(Question question) {
        validateContentHasText(question.getContent(), "Question content is invalid");

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
    }

    private void validatePublishable(Assessment assessment) {
        if (assessment.getItems() == null || assessment.getItems().isEmpty()) {
            throw new BadRequestException("Assessment must contain at least 1 question before publishing");
        }

        for (AssessmentItem item : assessment.getItems()) {
            validateContentHasText(item.getContent(), "Assessment item content is invalid");
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
}
