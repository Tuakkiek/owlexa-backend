package com.owlexa.owlexabackend.modules.payment.service;
import com.owlexa.owlexabackend.modules.payment.dto.request.FeeRecordGenerateRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestLevel;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmissionStatus;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.essay.entity.EssayGradingResult;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptStatus;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.DeviceTypeConverter;
import com.owlexa.owlexabackend.modules.essay.entity.EssayCriteriaScore;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmission;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubric;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.teacher.entity.BulkTeacherStatus;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.document.entity.DocumentType;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttempt;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestAttemptAnswer;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubricCriterion;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTest;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTestQuestion;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
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
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        validateMonth(request.getMonth());

        if (feeRecordRepository.existsByClazz_IdAndMonth(classId, request.getMonth())) {
            throw new DuplicateResourceException("Fee records already exist for this class and month");
        }

        List<ClassEnrollment> activeEnrollments = classEnrollmentRepository
                .findAllByClazz_IdAndStatus(classId, EnrollmentStatus.ACTIVE);

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

        assertCanViewFees(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        validateMonth(month);

        return feeRecordRepository.findAllByClazz_IdAndMonth(classId, month)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findMyFees() {
        User currentUser = getCurrentUser();
        return feeRecordRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Statuses that indicate the student still owes money (used for unpaid/overdue lists). */
    private static final List<FeeStatus> NOT_FULLY_PAID = List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL);

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

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> findAllPending() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCanViewFees(currentUser, centerId);

        return feeRecordRepository
                .findAllByCenter_IdAndStatusInOrderByCreatedAtDesc(centerId, NOT_FULLY_PAID)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FeeRecordResponse toResponse(FeeRecord feeRecord) {
        BigDecimal paid = feeRecord.getPaidAmount() != null ? feeRecord.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(discount);
        BigDecimal remaining = effectiveAmount.subtract(paid);

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
                .discountAmount(discount)
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
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
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