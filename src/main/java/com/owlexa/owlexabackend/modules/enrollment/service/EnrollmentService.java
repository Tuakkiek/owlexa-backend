package com.owlexa.owlexabackend.modules.enrollment.service;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public EnrollmentResponse enroll (Long classId, EnrollmentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser,centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        if (clazz.getStatus() != com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.OPEN
                && clazz.getStatus() != com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Class is not open for enrollment. Current status: " + clazz.getStatus());
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getStudentId()
                ));

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("User is not a student");
        }

        if (classEnrollmentRepository.existsByClazz_IdAndStudentUser_Id(classId, student.getId())) {
            throw new DuplicateResourceException("Student is already enrolled in this class");
        }

        long pendingOrActiveCount = classEnrollmentRepository.countByClazz_IdAndStatusIn(
                classId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE)
        );

        if (pendingOrActiveCount >= clazz.getMaxStudents()) {
            throw new BusinessRuleException("Class is full");
        }

        // Check student schedule conflicts (log warning, non-blocking)
        List<com.owlexa.owlexabackend.modules.class_management.entity.Schedule> classSchedules =
                scheduleRepository.findAllByClazz_IdAndCenter_Id(classId, centerId);
        for (var schedule : classSchedules) {
            long conflict = scheduleRepository.countOverlappingStudentSchedules(
                    student.getId(), schedule.getDayOfWeek(),
                    schedule.getStartTime(), schedule.getEndTime(), centerId);
            if (conflict > 0) {
                throw new BusinessRuleException(
                        "Student has a schedule conflict on " + schedule.getDayOfWeek()
                );
            }
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .clazz(clazz)
                .studentUser(student)
                .center(clazz.getCenter())
                .enrolledByUser(currentUser)
                .status(EnrollmentStatus.PENDING)
                .build();

        enrollment = classEnrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse approve(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for studentId: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING enrollments can be approved. Current status: " + enrollment.getStatus());
        }

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = classEnrollmentRepository.save(enrollment);

        // Auto-generate FeeRecord
        generateFeeRecordIfAbsent(enrollment);

        return toResponse(enrollment);
    }

    @Transactional
    public void reject(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for studentId: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING enrollments can be rejected. Current status: " + enrollment.getStatus());
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        classEnrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAllByClass(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        return classEnrollmentRepository.findAllByClazz_IdAndStatusIn(
                        classId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE))
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
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found for studentId: " + studentUserId
                ));
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return;
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        classEnrollmentRepository.save(enrollment);
    }

    // Helper: auto-generate FeeRecord when enrollment becomes ACTIVE
    private void generateFeeRecordIfAbsent(ClassEnrollment enrollment) {
        String currentMonth = YearMonth.now().toString();

        boolean alreadyExists = feeRecordRepository
                .findByStudentUser_IdAndClazz_IdAndMonth(
                        enrollment.getStudentUser().getId(),
                        enrollment.getClazz().getId(),
                        currentMonth
                ).isPresent();

        if (!alreadyExists && enrollment.getClazz().getMonthlyFee() != null
                && enrollment.getClazz().getMonthlyFee() > 0) {
            FeeRecord feeRecord = FeeRecord.builder()
                    .studentUser(enrollment.getStudentUser())
                    .center(enrollment.getCenter())
                    .clazz(enrollment.getClazz())
                    .amount(BigDecimal.valueOf(enrollment.getClazz().getMonthlyFee()))
                    .paidAmount(BigDecimal.ZERO)
                    .month(currentMonth)
                    .dueDate(LocalDate.now().plusDays(7))
                    .status(FeeStatus.UNPAID)
                    .build();
            feeRecordRepository.save(feeRecord);
        }
    }

    // To response
    private EnrollmentResponse toResponse(ClassEnrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .classId(enrollment.getClazz().getId())
                .centerId(enrollment.getCenter().getId())
                .studentUserId(enrollment.getStudentUser().getId())
                .studentPhoneNumber(enrollment.getStudentUser().getPhoneNumber())
                .studentFullName(enrollment.getStudentUser().getFullName())
                .enrollmentByUserId(enrollment.getEnrolledByUser() != null ? enrollment.getEnrolledByUser().getId() : null)
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
        Long centerId = TenantContext.getCurrentTenantId();

        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
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
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);

        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }
}
