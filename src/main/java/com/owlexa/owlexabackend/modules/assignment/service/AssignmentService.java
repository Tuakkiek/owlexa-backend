package com.owlexa.owlexabackend.modules.assignment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.service.FileReferenceService;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentType;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.assessment_builder.repository.AssessmentRepository;
import com.owlexa.owlexabackend.modules.assignment.dto.request.AssignmentRequest;
import com.owlexa.owlexabackend.modules.assignment.dto.request.AssignmentTargetRequest;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipientStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTarget;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentTargetType;
import com.owlexa.owlexabackend.modules.assignment.mapper.AssignmentMapper;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRecipientRepository;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentRepository;
import com.owlexa.owlexabackend.modules.assignment.repository.AssignmentSpecifications;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?:nbsp|#160);", Pattern.CASE_INSENSITIVE);

    private final AssignmentRepository assignmentRepository;
    private final AssignmentRecipientRepository assignmentRecipientRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final AssignmentMapper assignmentMapper;
    private final RichTextDocumentService richTextDocumentService;
    private final FileReferenceService fileReferenceService;

    @Transactional(readOnly = true)
    public Page<AssignmentListResponse> findAllForTeacher(
            String search,
            AssessmentType type,
            AssignmentStatus status,
            Long classId,
            Pageable pageable
    ) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assignmentRepository.findAll(
                        AssignmentSpecifications.search(centerId, search, type, status, classId),
                        pageable
                )
                .map(assignmentMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public AssignmentDetailResponse findByIdForTeacher(Long assignmentId) {
        requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assignmentMapper.toDetailResponse(findActiveAssignment(assignmentId, centerId));
    }

    @Transactional
    public AssignmentDetailResponse create(AssignmentRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateRequest(request);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));
        Assessment assessment = findPublishedAssessment(request.getAssessmentId(), centerId);

        Assignment assignment = Assignment.builder()
                .center(center)
                .assessment(assessment)
                .type(assessment.getType())
                .status(AssignmentStatus.DRAFT)
                .title(request.getTitle().trim())
                .description(normalizeOptionalText(request.getDescription()))
                .contentJson(assessment.getContentJson())
                .openAt(request.getOpenAt())
                .dueAt(request.getDueAt())
                .attemptLimit(request.getAttemptLimit())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        replaceTargets(assignment, request.getTargets(), centerId);

        return assignmentMapper.toDetailResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentDetailResponse update(Long assignmentId, AssignmentRequest request) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();
        validateRequest(request);

        Assignment assignment = findActiveAssignment(assignmentId, centerId);
        requireDraft(assignment, "Only draft assignments can be updated");
        Assessment assessment = findPublishedAssessment(request.getAssessmentId(), centerId);

        assignment.setAssessment(assessment);
        assignment.setType(assessment.getType());
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(normalizeOptionalText(request.getDescription()));
        assignment.setContentJson(assessment.getContentJson());
        assignment.setOpenAt(request.getOpenAt());
        assignment.setDueAt(request.getDueAt());
        assignment.setAttemptLimit(request.getAttemptLimit());
        assignment.setUpdatedBy(currentUser);

        replaceTargets(assignment, request.getTargets(), centerId);

        return assignmentMapper.toDetailResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentDetailResponse publish(Long assignmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assignment assignment = findActiveAssignment(assignmentId, centerId);
        requireDraft(assignment, "Only draft assignments can be published");
        validatePublishable(assignment);

        Instant now = Instant.now();
        rebuildAssignmentSnapshot(assignment, now);
        materializeRecipients(assignment, now);
        assignment.setStatus(resolvePublishedStatus(assignment, now));
        assignment.setUpdatedBy(currentUser);

        Assignment saved = assignmentRepository.save(assignment);
        fileReferenceService.syncReferences(
                FileOwnerType.ASSIGNMENT,
                saved.getId(),
                centerId,
                assignmentReferenceDocuments(saved),
                assignmentReferencedFileIds(saved)
        );
        return assignmentMapper.toDetailResponse(saved);
    }

    @Transactional
    public AssignmentDetailResponse close(Long assignmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assignment assignment = findActiveAssignment(assignmentId, centerId);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE && assignment.getStatus() != AssignmentStatus.SCHEDULED) {
            throw new BadRequestException("Only active or scheduled assignments can be closed");
        }

        assignment.setStatus(AssignmentStatus.CLOSED);
        assignment.setUpdatedBy(currentUser);
        return assignmentMapper.toDetailResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentDetailResponse archive(Long assignmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assignment assignment = findActiveAssignment(assignmentId, centerId);
        if (assignment.getStatus() != AssignmentStatus.CLOSED) {
            throw new BadRequestException("Only closed assignments can be archived");
        }

        assignment.setStatus(AssignmentStatus.ARCHIVED);
        assignment.setUpdatedBy(currentUser);
        return assignmentMapper.toDetailResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void delete(Long assignmentId) {
        User currentUser = requireTeacherInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        Assignment assignment = findActiveAssignment(assignmentId, centerId);
        requireDraft(assignment, "Only draft assignments can be deleted");
        assignment.setDeletedAt(Instant.now());
        assignment.setUpdatedBy(currentUser);
        assignmentRepository.save(assignment);
        fileReferenceService.syncReferences(
                FileOwnerType.ASSIGNMENT,
                assignment.getId(),
                centerId,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public List<StudentAssignmentListResponse> findAllForStudent() {
        User currentUser = requireStudentInCurrentCenter();
        Long centerId = requiredCurrentCenterId();

        return assignmentRecipientRepository
                .findAllByStudentUser_IdAndAssignment_Center_IdAndAssignment_DeletedAtIsNullOrderByAssignedAtDesc(
                        currentUser.getId(),
                        centerId
                )
                .stream()
                .map(assignmentMapper::toStudentListResponse)
                .toList();
    }

    private void validateRequest(AssignmentRequest request) {
        if (request.getAssessmentId() == null) {
            throw new BadRequestException("Assessment id is required");
        }
        validateContentHasText(request.getTitle(), "Assignment title is required");
        if (request.getOpenAt() != null && request.getDueAt() != null && !request.getOpenAt().isBefore(request.getDueAt())) {
            throw new BadRequestException("Open time must be before due time");
        }
        if (request.getAttemptLimit() != null && request.getAttemptLimit() < 1) {
            throw new BadRequestException("Attempt limit must be greater than or equal to 1");
        }
        validateTargetRequests(request.getTargets());
    }

    private List<JsonNode> assignmentReferenceDocuments(Assignment assignment) {
        List<JsonNode> documents = new ArrayList<>();
        documents.add(richTextDocumentService.deserialize(assignment.getContentJson()));
        for (AssignmentItem item : assignment.getItems()) {
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

    private void validateTargetRequests(List<AssignmentTargetRequest> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new BadRequestException("Assignment must have at least 1 target");
        }

        Set<Long> classTargets = new HashSet<>();
        Set<Long> studentTargets = new HashSet<>();

        for (AssignmentTargetRequest target : targets) {
            if (target == null || target.getTargetType() == null) {
                throw new BadRequestException("Assignment target is invalid");
            }
            if (target.getTargetType() == AssignmentTargetType.CLASS) {
                if (target.getClassId() == null || target.getStudentUserId() != null) {
                    throw new BadRequestException("Class target must include classId only");
                }
                if (!classTargets.add(target.getClassId())) {
                    throw new BadRequestException("Duplicate class target is not allowed");
                }
                continue;
            }
            if (target.getTargetType() == AssignmentTargetType.STUDENT) {
                if (target.getStudentUserId() == null || target.getClassId() != null) {
                    throw new BadRequestException("Student target must include studentUserId only");
                }
                if (!studentTargets.add(target.getStudentUserId())) {
                    throw new BadRequestException("Duplicate student target is not allowed");
                }
            }
        }
    }

    private void replaceTargets(Assignment assignment, List<AssignmentTargetRequest> targetRequests, Long centerId) {
        assignment.getTargets().clear();
        targetRequests.stream()
                .map(request -> toTarget(assignment, request, centerId))
                .forEach(assignment.getTargets()::add);
    }

    private AssignmentTarget toTarget(Assignment assignment, AssignmentTargetRequest request, Long centerId) {
        if (request.getTargetType() == AssignmentTargetType.CLASS) {
            com.owlexa.owlexabackend.modules.class_management.entity.Class clazz = classRepository
                    .findByIdAndCenter_Id(request.getClassId(), centerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getClassId()));
            if (clazz.getStatus() != ClassStatus.ACTIVE) {
                throw new BadRequestException("Class target must be active");
            }
            return AssignmentTarget.builder()
                    .assignment(assignment)
                    .targetType(AssignmentTargetType.CLASS)
                    .clazz(clazz)
                    .build();
        }

        User student = userRepository.findById(request.getStudentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentUserId()));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Target user must be a student");
        }
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(student.getId(), centerId);
        if (!hasMembership) {
            throw new ResourceNotFoundException("Student not found with id: " + request.getStudentUserId());
        }

        return AssignmentTarget.builder()
                .assignment(assignment)
                .targetType(AssignmentTargetType.STUDENT)
                .studentUser(student)
                .build();
    }

    private void validatePublishable(Assignment assignment) {
        Assessment assessment = assignment.getAssessment();
        if (assessment == null || assessment.getStatus() != AssessmentStatus.PUBLISHED || assessment.getDeletedAt() != null) {
            throw new BadRequestException("Assignment assessment must be published");
        }
        if (assessment.getItems() == null || assessment.getItems().isEmpty()) {
            throw new BadRequestException("Assessment must contain at least 1 question");
        }
        if (assignment.getTargets() == null || assignment.getTargets().isEmpty()) {
            throw new BadRequestException("Assignment must have at least 1 target");
        }
    }

    private void rebuildAssignmentSnapshot(Assignment assignment, Instant snapshotAt) {
        assignment.getItems().clear();
        Assessment assessment = assignment.getAssessment();
        assignment.getAssessment().getItems().stream()
                .sorted(Comparator.comparing(com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem::getDisplayOrder))
                .map(assignmentMapper::toItemSnapshot)
                .forEach(item -> {
                    item.setAssignment(assignment);
                    assignment.getItems().add(item);
                });
        assignment.setAudioFile(assessment.getAudioFile());
        assignment.setPlaybackMode(assessment.getPlaybackMode() == null ? PlaybackMode.PRACTICE : assessment.getPlaybackMode());
        assignment.setContentJson(assessment.getContentJson());
        assignment.setAssessmentSnapshotAt(snapshotAt);
    }

    private List<Long> assignmentReferencedFileIds(Assignment assignment) {
        StoredFile audioFile = assignment.getAudioFile();
        return audioFile == null ? List.of() : List.of(audioFile.getId());
    }

    private void materializeRecipients(Assignment assignment, Instant assignedAt) {
        assignment.getRecipients().clear();

        Map<Long, AssignmentRecipient> recipientsByStudentId = new LinkedHashMap<>();
        for (AssignmentTarget target : assignment.getTargets()) {
            if (target.getTargetType() == AssignmentTargetType.CLASS) {
                List<ClassEnrollment> enrollments = classEnrollmentRepository
                        .findAllByClazz_IdAndStatus(target.getClazz().getId(), EnrollmentStatus.ACTIVE);
                for (ClassEnrollment enrollment : enrollments) {
                    recipientsByStudentId.putIfAbsent(
                            enrollment.getStudentUser().getId(),
                            buildRecipient(
                                    assignment,
                                    enrollment.getStudentUser(),
                                    target.getClazz(),
                                    AssignmentTargetType.CLASS,
                                    assignedAt
                            )
                    );
                }
            } else {
                recipientsByStudentId.putIfAbsent(
                        target.getStudentUser().getId(),
                        buildRecipient(
                                assignment,
                                target.getStudentUser(),
                                null,
                                AssignmentTargetType.STUDENT,
                                assignedAt
                        )
                );
            }
        }

        if (recipientsByStudentId.isEmpty()) {
            throw new BadRequestException("Assignment targets must produce at least 1 recipient");
        }

        assignment.getRecipients().addAll(recipientsByStudentId.values());
    }

    private AssignmentRecipient buildRecipient(
            Assignment assignment,
            User student,
            com.owlexa.owlexabackend.modules.class_management.entity.Class clazz,
            AssignmentTargetType sourceType,
            Instant assignedAt
    ) {
        return AssignmentRecipient.builder()
                .assignment(assignment)
                .studentUser(student)
                .clazz(clazz)
                .sourceType(sourceType)
                .status(AssignmentRecipientStatus.ASSIGNED)
                .assignedAt(assignedAt)
                .build();
    }

    private AssignmentStatus resolvePublishedStatus(Assignment assignment, Instant now) {
        return assignment.getOpenAt() != null && assignment.getOpenAt().isAfter(now)
                ? AssignmentStatus.SCHEDULED
                : AssignmentStatus.ACTIVE;
    }

    private Assessment findPublishedAssessment(Long assessmentId, Long centerId) {
        Assessment assessment = assessmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(assessmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + assessmentId));
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new BadRequestException("Assignment assessment must be published");
        }
        return assessment;
    }

    private Assignment findActiveAssignment(Long assignmentId, Long centerId) {
        return assignmentRepository.findByIdAndCenter_IdAndDeletedAtIsNull(assignmentId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
    }

    private void requireDraft(Assignment assignment, String message) {
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new BadRequestException(message);
        }
    }

    private User requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage assignments");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }

        return currentUser;
    }

    private User requireStudentInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only STUDENT can view assigned work");
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
