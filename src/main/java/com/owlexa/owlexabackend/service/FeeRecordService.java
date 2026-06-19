package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.FeeRecordGenerateRequest;
import com.owlexa.owlexabackend.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.entity.*;
import com.owlexa.owlexabackend.entity.Class;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.repository.ClassRepository;
import com.owlexa.owlexabackend.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeRecordService {

    private final FeeRecordRepository feeRecordRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional
    public List<FeeRecordResponse> generateForClass(Long classId, FeeRecordGenerateRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this class");
        }

        validateMonth(request.getMonth());

        if (feeRecordRepository.existsByClazzIdAndMonth(classId, request.getMonth())) {
            throw new DuplicateResourceException("Fee records already exist for this class and month");
        }

        List<ClassEnrollment> activeEnrollments = classEnrollmentRepository
                .findAllByClazzIdAndStatus(classId, EnrollmentStatus.ACTIVE);

        if (activeEnrollments.isEmpty()) {
            throw new BadRequestException("Class has no active students");
        }

        BigDecimal amount = BigDecimal.valueOf(clazz.getMonthlyFee());

        List<FeeRecord> feeRecords = activeEnrollments.stream()
                .map(enrollment -> FeeRecord.builder()
                        .studentUser(enrollment.getStudentUser())
                        .center(clazz.getCenter())
                        .clazz(clazz)
                        .amount(amount)
                        .paidAmount(BigDecimal.ZERO)
                        .month(request.getMonth())
                        .dueDate(request.getDueDate())
                        .status(FeeStatus.UNPAID)
                        .build())
                .toList();

        List<FeeRecord> saved = feeRecordRepository.saveAll(feeRecords);

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findAllByClass(Long classId, String month) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to view this class");
        }

        validateMonth(month);

        return feeRecordRepository.findAllByClazzIdAndMonth(classId, month)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findMyFees() {
        User currentUser = getCurrentUser();
        return feeRecordRepository.findAllByStudentUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findAllOverdue() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        return feeRecordRepository
                .findAllByCenterIdAndStatusAndDueDateBefore(centerId, FeeStatus.UNPAID, java.time.LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FeeRecordResponse toResponse(FeeRecord feeRecord) {
        return FeeRecordResponse.builder()
                .id(feeRecord.getId())
                .studentUserId(feeRecord.getStudentUser().getId())
                .studentPhoneNumber(feeRecord.getStudentUser().getPhoneNumber())
                .studentFullName(feeRecord.getStudentUser().getFullName())
                .centerId(feeRecord.getCenter().getId())
                .classId(feeRecord.getClazz().getId())
                .className(feeRecord.getClazz().getName())
                .amount(feeRecord.getAmount())
                .paidAmount(feeRecord.getPaidAmount())
                .month(feeRecord.getMonth())
                .dueDate(feeRecord.getDueDate())
                .status(feeRecord.getStatus())
                .createdAt(feeRecord.getCreatedAt())
                .build();
    }

    private void validateMonth(String month) {
        try {
            YearMonth.parse(month);
        } catch (Exception ex) {
            throw new BadRequestException("month must have format YYYY-MM");
        }
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
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
            throw new AccessDeniedException("Only OWNER can manage fee records");
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