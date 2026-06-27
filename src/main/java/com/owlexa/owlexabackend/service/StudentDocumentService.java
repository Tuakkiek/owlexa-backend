package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.StudentDocumentRequest;
import com.owlexa.owlexabackend.dto.response.StudentDocumentResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Class;
import com.owlexa.owlexabackend.entity.ClassEnrollment;
import com.owlexa.owlexabackend.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.StudentDocument;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.repository.ClassRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.StudentDocumentRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
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

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> findMyDocuments() {
        User currentUser = requireCurrentUser(Role.STUDENT);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return classEnrollmentRepository.findAllByStudentUserIdAndCenterId(currentUser.getId(), centerId)
                .stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(ClassEnrollment::getClazz)
                .flatMap(clazz -> studentDocumentRepository
                        .findAllByClazzIdAndCenterIdOrderByUploadedAtDesc(clazz.getId(), centerId)
                        .stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentDocumentResponse createForClass(Long classId, StudentDocumentRequest request) {
        User currentUser = requireCurrentUser(Role.OWNER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getUrl() == null || request.getUrl().isBlank()
                || request.getType() == null) {
            throw new BadRequestException("Document title, type and url are required");
        }

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this class");
        }
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        StudentDocument document = StudentDocument.builder()
                .title(request.getTitle().trim())
                .type(request.getType())
                .url(request.getUrl().trim())
                .clazz(clazz)
                .center(center)
                .uploadedByUser(currentUser)
                .build();

        return toResponse(studentDocumentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> findClassDocuments(Long classId) {
        User currentUser = requireCurrentUser(Role.OWNER);
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this class");
        }

        return studentDocumentRepository.findAllByClazzIdAndCenterIdOrderByUploadedAtDesc(classId, centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StudentDocumentResponse toResponse(StudentDocument document) {
        return StudentDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .type(document.getType())
                .uploadedAt(document.getUploadedAt())
                .url(document.getUrl())
                .classId(document.getClazz().getId())
                .className(document.getClazz().getName())
                .build();
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
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        if (!membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId)) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }
}
