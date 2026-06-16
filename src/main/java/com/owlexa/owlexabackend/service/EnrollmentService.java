package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.entity.*;
import com.owlexa.owlexabackend.entity.Class;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;

    @Transactional
    public EnrollmentResponse enroll (Long classId, EnrollmentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser,centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));


        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this class");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getStudentId()
                ));

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("User is not a student");
        }

        if (classEnrollmentRepository.existsByClazzIdAndStudentUserId(classId, student.getId())) {
            throw new DuplicateResourceException("Student is already exists in this class");
        }

        long activeEnrollmentCount = classEnrollmentRepository.countByClazzIdAndStatus(
                classId, EnrollmentStatus.ACTIVE
        );

        if (activeEnrollmentCount >= clazz.getMaxStudents()) {
            throw new BadRequestException("Class is full");
        }

        var existingEnrollment = classEnrollmentRepository.findByClazzIdAndStudentUserId(classId, student.getId());

        ClassEnrollment enrollment;
        if (existingEnrollment.isPresent()) {
            enrollment = existingEnrollment.get();

            if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new DuplicateResourceException("Student is already enrolled in this class");
            }

            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setEnrolledByUser(currentUser);
        } else {
            enrollment = ClassEnrollment.builder()
                    .clazz(clazz)
                    .studentUser(student)
                    .center(clazz.getCenter())
                    .enrolledByUser(currentUser)
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
        }

        enrollment = classEnrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAllByClass(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage to view this class");
        }

        return classEnrollmentRepository.findAllByClazzIdAndStatus(classId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void drop (Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if(!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this class");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazzIdAndStudentUserId(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for studentId: " + studentUserId
                ));
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return;
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        classEnrollmentRepository.save(enrollment);
    }

    @Transactional
    public void remove(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if(!clazz.getCenter().getId().equals(classId)) {
            throw new AccessDeniedException("You do not permission to manage this class");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazzIdAndStudentUserId(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for studentId: " +studentUserId));

        classEnrollmentRepository.delete(enrollment);
    }

    // Helper
    // To response
    private EnrollmentResponse toResponse(ClassEnrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .classId(enrollment.getClazz().getId())
                .centerId(enrollment.getCenter().getId())
                .studentUserId(enrollment.getStudentUser().getId())
                .studentPhoneNumber(enrollment.getStudentUser().getPhoneNumber())
                .studentFullName(enrollment.getStudentUser().getFullName())
                .enrollmentByUserId(enrollment.getEnrolledByUser().getId())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Current User not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantFilter.getCurrentCenterId();

        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }

        return centerId;
    }

    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage enrollments");
        }

        assertCenterMembership(currentUser, centerId);
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);

        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }
}
