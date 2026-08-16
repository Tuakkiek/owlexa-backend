package com.owlexa.owlexabackend.modules.payment.service;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.dto.request.UpdateDueDateRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeRecordService {

    private final FeeRecordRepository feeRecordRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findMyFees() {
        User currentUser = getCurrentUser();
        return feeRecordRepository.findAllActiveEnrollmentFeesByStudentUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Statuses that indicate the student still owes money (used for unpaid/overdue lists). */
    private static final List<FeeStatus> NOT_FULLY_PAID =
            List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL, FeeStatus.OVERDUE);

    /**
     * Includes CANCELLED only for repairing rows left behind by a drop/enroll
     * race. The repository query still requires an active enrollment.
     */
    private static final List<FeeStatus> PENDING_WITH_RECOVERY =
            List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL, FeeStatus.OVERDUE, FeeStatus.CANCELLED);

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findAllOverdue() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCanViewFees(currentUser, centerId);

        return feeRecordRepository
                .findAllByCenter_IdAndStatusInAndDueDateBefore(centerId, NOT_FULLY_PAID, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<FeeRecordResponse> findAllPending() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCanViewFees(currentUser, centerId);

        List<FeeRecord> records = feeRecordRepository
                .findAllByCenter_IdAndStatusInOrderByCreatedAtDesc(centerId, PENDING_WITH_RECOVERY)
                .stream()
                .filter(record -> record.getStatus() != FeeStatus.CANCELLED
                        || isUnpaid(record))
                .toList();

        // A dropped enrollment cancels unpaid fees. If the enrollment is active
        // again, the fee is collectible and must be visible to the cashier.
        // Repairing here also fixes rows created by older concurrent requests.
        records.stream()
                .filter(record -> record.getStatus() == FeeStatus.CANCELLED)
                .forEach(record -> {
                    record.setStatus(FeeStatus.UNPAID);
                    feeRecordRepository.save(record);
                });

        return records
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isUnpaid(FeeRecord record) {
        return record.getPaidAmount() == null
                || record.getPaidAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findByClass(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCanViewFees(currentUser, centerId);

        return feeRecordRepository
                .findAllByCenter_IdAndClazz_IdOrderByCreatedAtDesc(centerId, classId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<FeeRecordResponse> updateClassFeeDueDate(Long classId, UpdateDueDateRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCanViewFees(currentUser, centerId);

        List<FeeRecord> records = feeRecordRepository
                .findAllByCenter_IdAndClazz_IdOrderByCreatedAtDesc(centerId, classId);

        for (FeeRecord record : records) {
            // Cập nhật hạn đóng học phí cho tất cả bản ghi chưa thu đủ
            if (record.getStatus() != FeeStatus.PAID) {
                record.setDueDate(request.getDueDate());
            }
        }
        List<FeeRecord> saved = feeRecordRepository.saveAll(records);
        return saved.stream().map(this::toResponse).toList();
    }

    private FeeRecordResponse toResponse(FeeRecord feeRecord) {
        BigDecimal paid = feeRecord.getPaidAmount() != null ? feeRecord.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = feeRecord.getAmount().subtract(paid);

        FeeStatus effectiveStatus = resolveEffectiveStatus(feeRecord.getStatus(), feeRecord.getDueDate());

        // Look up enrollment status so the student portal can surface SUSPENDED state
        EnrollmentStatus enrollmentStatus = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(
                        feeRecord.getClazz().getId(),
                        feeRecord.getStudentUser().getId())
                .map(ClassEnrollment::getStatus)
                .orElse(null);

        return FeeRecordResponse.builder()
                .id(feeRecord.getId())
                .studentUserId(feeRecord.getStudentUser().getId())
                .studentPhoneNumber(feeRecord.getStudentUser().getPhoneNumber())
                .studentFullName(feeRecord.getStudentUser().getFullName())
                .centerId(feeRecord.getCenter().getId())
                .classId(feeRecord.getClazz().getId())
                .className(feeRecord.getClazz().getName())
                .amount(feeRecord.getAmount())
                .paidAmount(paid)
                .remainingAmount(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining)
                .month(feeRecord.getMonth())
                .dueDate(feeRecord.getDueDate())
                .status(effectiveStatus)
                .enrollmentStatus(enrollmentStatus)
                .createdAt(feeRecord.getCreatedAt())
                .build();
    }

    /**
     * Computes the effective status for display.
     * If the stored status is UNPAID or PARTIAL and the due date has passed,
     * the effective status is OVERDUE. Otherwise the stored status is used.
     */
    private FeeStatus resolveEffectiveStatus(FeeStatus storedStatus, LocalDate dueDate) {
        if (dueDate == null) return storedStatus;
        if (storedStatus == FeeStatus.UNPAID || storedStatus == FeeStatus.PARTIAL) {
            if (dueDate.isBefore(LocalDate.now())) {
                return FeeStatus.OVERDUE;
            }
        }
        return storedStatus;
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
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    private void assertCanViewFees(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can view fee records");
        }

        assertCenterMembership(currentUser, centerId);
    }
}

