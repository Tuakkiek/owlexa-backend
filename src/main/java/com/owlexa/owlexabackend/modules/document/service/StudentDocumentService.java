package com.owlexa.owlexabackend.modules.document.service;
import com.owlexa.owlexabackend.modules.document.dto.request.StudentDocumentRequest;
import com.owlexa.owlexabackend.modules.document.dto.response.StudentDocumentResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.document.repository.StudentDocumentRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentDocumentService {

    private final StudentDocumentRepository studentDocumentRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleEventRepository scheduleEventRepository;

    // ── Student: view own documents ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> findMyDocuments() {
        User currentUser = requireCurrentUser(Role.STUDENT);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return classEnrollmentRepository.findAllByStudentUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(ClassEnrollment::getClazz)
                .flatMap(clazz -> studentDocumentRepository
                        .findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(clazz.getId(), centerId)
                        .stream())
                .map(this::toResponse)
                .toList();
    }

    // ── Owner: create document for class ─────────────────────────────────────

    @Transactional
    public StudentDocumentResponse createForClass(Long classId, StudentDocumentRequest request) {
        User currentUser = requireCurrentUser(Role.OWNER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return doCreateForClass(classId, request, currentUser, centerId);
    }

    // ── Owner: view class documents ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> findClassDocuments(Long classId) {
        User currentUser = requireCurrentUser(Role.OWNER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        return studentDocumentRepository.findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(classId, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Teacher: create document for own class ───────────────────────────────

    @Transactional
    public StudentDocumentResponse createForClassAsTeacher(Long classId, StudentDocumentRequest request) {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);
        assertTeacherTeachesClass(currentUser, classId, centerId);

        return doCreateForClass(classId, request, currentUser, centerId);
    }

    // ── Teacher: view own class documents ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> findClassDocumentsAsTeacher(Long classId) {
        User currentUser = requireCurrentUser(Role.TEACHER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);
        assertTeacherTeachesClass(currentUser, classId, centerId);

        return studentDocumentRepository.findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(classId, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Shared create logic ──────────────────────────────────────────────────

    private StudentDocumentResponse doCreateForClass(Long classId, StudentDocumentRequest request,
                                                     User currentUser, Long centerId) {
        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getUrl() == null || request.getUrl().isBlank()
                || request.getType() == null) {
            throw new BadRequestException("Document title, type and url are required");
        }

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        StudentDocument document = StudentDocument.builder()
                .title(request.getTitle().trim())
                .documentType(request.getType())
                .fileUrl(request.getUrl().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .clazz(clazz)
                .center(center)
                .uploaderUser(currentUser)
                .build();

        return toResponse(studentDocumentRepository.save(document));
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private StudentDocumentResponse toResponse(StudentDocument document) {
        return StudentDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .type(document.getDocumentType())
                .uploadedAt(document.getCreatedAt())
                .url(document.getFileUrl())
                .classId(document.getClazzId())
                .className(document.getClazz() != null ? document.getClazz().getName() : null)
                .uploaderName(document.getUploaderUser() != null ? document.getUploaderUser().getFullName() : null)
                .description(document.getDescription())
                .build();
    }

    // ── Security helpers ─────────────────────────────────────────────────────

    private void assertTeacherTeachesClass(User teacher, Long classId, Long centerId) {
        boolean teaches = scheduleEventRepository
                .findAllByTeacherUser_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(teacher.getId(), centerId)
                .stream()
                .filter(event -> event.getStatus() != ScheduleEventStatus.CANCELLED)
                .anyMatch(event -> event.getClazz() != null && event.getClazz().getId().equals(classId));
        if (!teaches) {
            throw new AccessDeniedException("Teacher is not assigned to teach class " + classId);
        }
    }

    private User requireCurrentUser(Role role) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != role) {
            throw new AccessDeniedException("Only " + role + " can access this resource");
        }
        return currentUser;
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        if (!membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }
}
