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

        long currentEnrollmentCount = classEnrollmentRepository.countByClazzId(classId);
        if (currentEnrollmentCount >= clazz.getMaxStudents()) {
            throw new BadRequestException("Class is full");
        }

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .clazz(clazz)
                .studentUser(student)
                .center(center)
                .enrolledByUser(currentUser)
                .build();

        return toResponse(classEnrollmentRepository.save(enrollment));
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
