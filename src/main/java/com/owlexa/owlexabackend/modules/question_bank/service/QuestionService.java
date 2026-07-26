package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.grading_criteria.entity.GradingCriteria;
import com.owlexa.owlexabackend.modules.grading_criteria.repository.GradingCriteriaRepository;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionOptionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.GradingCriteriaSummaryResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionOptionResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionOption;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?:nbsp|#160);", Pattern.CASE_INSENSITIVE);

    private final QuestionRepository questionRepository;
    private final GradingCriteriaRepository gradingCriteriaRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Page<QuestionResponse> findAll(
            String search,
            QuestionType type,
            QuestionDifficulty difficulty,
            Long gradingCriteriaId,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return questionRepository.findAll(
                        QuestionSpecifications.search(centerId, search, type, difficulty, gradingCriteriaId),
                        pageable
                )
                .map(question -> toResponse(question, false));
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(Long questionId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return toResponse(findActiveQuestion(questionId, centerId), true);
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateQuestionRequest(request);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));
        GradingCriteria gradingCriteria = resolveGradingCriteria(request, centerId);

        Question question = Question.builder()
                .center(center)
                .type(request.getType())
                .title(normalizeOptionalText(request.getTitle()))
                .content(request.getContent().trim())
                .difficulty(request.getDifficulty())
                .points(request.getPoints())
                .gradingCriteria(gradingCriteria)
                .explanation(normalizeOptionalText(request.getExplanation()))
                .sampleAnswer(normalizeOptionalText(request.getSampleAnswer()))
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        replaceOptions(question, request.getOptions());

        return toResponse(questionRepository.save(question), true);
    }

    @Transactional
    public QuestionResponse update(Long questionId, QuestionRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateQuestionRequest(request);

        Question question = findActiveQuestion(questionId, centerId);
        GradingCriteria gradingCriteria = resolveGradingCriteria(request, centerId);

        question.setType(request.getType());
        question.setTitle(normalizeOptionalText(request.getTitle()));
        question.setContent(request.getContent().trim());
        question.setDifficulty(request.getDifficulty());
        question.setPoints(request.getPoints());
        question.setGradingCriteria(gradingCriteria);
        question.setExplanation(normalizeOptionalText(request.getExplanation()));
        question.setSampleAnswer(normalizeOptionalText(request.getSampleAnswer()));
        question.setUpdatedBy(currentUser);

        replaceOptions(question, request.getOptions());

        return toResponse(questionRepository.save(question), true);
    }

    @Transactional
    public void delete(Long questionId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Question question = findActiveQuestion(questionId, centerId);
        question.setDeletedAt(Instant.now());
        question.setUpdatedBy(currentUser);
        questionRepository.save(question);
    }

    private void validateQuestionRequest(QuestionRequest request) {
        if (request.getType() == null) {
            throw new BadRequestException("Loại câu hỏi không được để trống");
        }
        validateContentHasText(request.getContent(), "Nội dung câu hỏi không được để trống");
        validatePoints(request);

        if (request.getType() == QuestionType.MULTIPLE_CHOICE) {
            validateMultipleChoice(request);
            return;
        }

        if (request.getType() == QuestionType.ESSAY) {
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
                validateContentHasText(option.getContent(), "Nội dung lựa chọn không được để trống")
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

    private QuestionResponse toResponse(Question question, boolean includeOptions) {
        GradingCriteria criteria = question.getGradingCriteria();

        return QuestionResponse.builder()
                .id(question.getId())
                .type(question.getType())
                .title(question.getTitle())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .points(question.getPoints())
                .gradingCriteria(criteria == null ? null : toGradingCriteriaSummary(criteria))
                .explanation(question.getExplanation())
                .sampleAnswer(question.getSampleAnswer())
                .options(includeOptions ? toOptionResponses(question) : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private GradingCriteriaSummaryResponse toGradingCriteriaSummary(GradingCriteria criteria) {
        return GradingCriteriaSummaryResponse.builder()
                .id(criteria.getId())
                .name(criteria.getName())
                .build();
    }

    private List<QuestionOptionResponse> toOptionResponses(Question question) {
        return question.getOptions().stream()
                .sorted(Comparator.comparing(QuestionOption::getDisplayOrder))
                .map(option -> QuestionOptionResponse.builder()
                        .id(option.getId())
                        .content(option.getContent())
                        .isCorrect(option.getIsCorrect())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();
    }
}
