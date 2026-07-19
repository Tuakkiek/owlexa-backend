package com.owlexa.owlexabackend.modules.payment.service;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.TimelineEntryResponse;
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
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.payment.entity.AuditLog;
import com.owlexa.owlexabackend.modules.payment.entity.Installment;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import com.owlexa.owlexabackend.modules.payment.entity.Refund;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import com.owlexa.owlexabackend.modules.payment.repository.DiscountRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final InstallmentRepository installmentRepository;
    private final RefundRepository refundRepository;
    private final DiscountRepository discountRepository;


    // Collect cash
    @Transactional
    public PaymentResponse collectCash(Long feeRecordId, CashPaymentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);
        assertCanCollectPayment(currentUser, centerId);

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if(!feeRecord.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Fee record " + feeRecordId + " belongs to another center");
        }

        validateAmount(request.getAmount());

        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(discount);
        BigDecimal remainingAmount = effectiveAmount.subtract(feeRecord.getPaidAmount());

        if(request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BusinessRuleException("Payment amount exceeds remaining balance");
        }

        PaymentMethod method = request.getMethod() != null ? request.getMethod() : PaymentMethod.CASH;
        String receiptNumber = generateReceiptNumber();

        Payment payment = Payment.builder()
                .receiptNumber(receiptNumber)
                .feeRecord(feeRecord)
                .center(feeRecord.getCenter())
                .studentUser(feeRecord.getStudentUser())
                .collectedByUser(currentUser)
                .amount(request.getAmount())
                .method(method)
                .note(normalizeText(request.getNote()))
                .status(TransactionStatus.ACTIVE)
                .build();

        payment = paymentRepository.save(payment);

        // Allocate to oldest unpaid installment if installments exist
        allocateToInstallments(feeRecord, request.getAmount());

        BigDecimal newPaidAmount = feeRecord.getPaidAmount().add(request.getAmount());
        feeRecord.setPaidAmount(newPaidAmount);
        feeRecord.setStatus(resloveFeeStatus(effectiveAmount, newPaidAmount));

        feeRecordRepository.save(feeRecord);

        // Audit log
        auditLog(currentUser, feeRecord.getCenter(), "PAYMENT_COLLECTED", "Payment",
                payment.getId(), "Collected " + request.getAmount() + " VND from " +
                        feeRecord.getStudentUser().getFullName() + " - Receipt: " + receiptNumber);

        return toResponse(payment, feeRecord);
    }

    // ── Paginated history with filtering ─────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAllPaginated(Long centerId,
                                                   String studentQuery,
                                                   Long cashierId,
                                                   PaymentMethod method,
                                                   Instant startDate,
                                                   Instant endDate,
                                                   Pageable pageable) {
        User currentUser = getCurrentUser();
        assertCenterMembership(currentUser, centerId);

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("center").get("id"), centerId));

            if (studentQuery != null && !studentQuery.isBlank()) {
                String pattern = "%" + studentQuery.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("studentUser").get("fullName")), pattern),
                        cb.like(cb.lower(root.get("studentUser").get("phoneNumber")), pattern)
                ));
            }
            if (cashierId != null) {
                predicates.add(cb.equal(root.get("collectedByUser").get("id"), cashierId));
            }
            if (method != null) {
                predicates.add(cb.equal(root.get("method"), method));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return paymentRepository.findAll(spec, pageable)
                .map(payment -> toResponse(payment, payment.getFeeRecord()));
    }

    // ── Receipt ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getReceipt(Long paymentId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCanViewPayments(currentUser, centerId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment " + paymentId + " belongs to another center");
        }

        return toResponse(payment, payment.getFeeRecord());
    }

    // ── Void payment ──────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse voidPayment(Long paymentId, String reason) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can void payments");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment belongs to another center");
        }
        if (payment.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessRuleException("Payment is already voided");
        }

        payment.setStatus(TransactionStatus.VOIDED);
        payment.setVoidReason(normalizeText(reason));
        payment.setVoidedBy(currentUser);
        payment.setVoidedAt(Instant.now());
        paymentRepository.save(payment);

        // Reverse the paid amount on fee record
        FeeRecord feeRecord = payment.getFeeRecord();
        feeRecord.setPaidAmount(feeRecord.getPaidAmount().subtract(payment.getAmount()));
        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount().subtract(discount), feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        auditLog(currentUser, payment.getCenter(), "PAYMENT_VOIDED", "Payment",
                payment.getId(), "Voided payment " + payment.getReceiptNumber() + " - Reason: " + reason);

        return toResponse(payment, feeRecord);
    }

    // ── Refund ────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse refund(Long paymentId, BigDecimal amount, String reason) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can process refunds");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment belongs to another center");
        }
        if (payment.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessRuleException("Cannot refund a voided payment");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be greater than 0");
        }

        BigDecimal alreadyRefunded = refundRepository.findAllByPaymentOrderByCreatedAtDesc(payment)
                .stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);
        if (amount.compareTo(maxRefundable) > 0) {
            throw new BusinessRuleException("Refund amount exceeds available balance");
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .center(payment.getCenter())
                .amount(amount)
                .reason(normalizeText(reason))
                .createdBy(currentUser)
                .build();
        refundRepository.save(refund);

        // Decrease paid amount
        FeeRecord feeRecord = payment.getFeeRecord();
        feeRecord.setPaidAmount(feeRecord.getPaidAmount().subtract(amount));
        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount().subtract(discount), feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        auditLog(currentUser, payment.getCenter(), "REFUND_CREATED", "Payment",
                payment.getId(), "Refunded " + amount + " VND from " + payment.getReceiptNumber() + " - Reason: " + reason);

        return toResponse(payment, feeRecord);
    }

    // ── Installment allocation ────────────────────────────────────────────

    private void allocateToInstallments(FeeRecord feeRecord, BigDecimal amount) {
        List<Installment> installments = installmentRepository.findOldestUnpaid(feeRecord,
                List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIALLY_PAID));
        BigDecimal remaining = amount;
        for (Installment inst : installments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal instRemaining = inst.getExpectedAmount().subtract(inst.getPaidAmount());
            BigDecimal toAllocate = remaining.min(instRemaining);
            inst.setPaidAmount(inst.getPaidAmount().add(toAllocate));
            BigDecimal instPaid = inst.getPaidAmount();
            if (instPaid.compareTo(inst.getExpectedAmount()) >= 0) {
                inst.setStatus(InstallmentStatus.PAID);
            } else if (instPaid.compareTo(BigDecimal.ZERO) > 0) {
                inst.setStatus(InstallmentStatus.PARTIALLY_PAID);
            }
            installmentRepository.save(inst);
            remaining = remaining.subtract(toAllocate);
        }
    }

    // Find all by feeRecordId
    @Transactional(readOnly = true)
    public List<PaymentResponse> findAllByFeeRecord(Long feeRecordId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if(!feeRecord.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Fee record " + feeRecordId + " belongs to another center");
        }

        return paymentRepository.findAllByFeeRecordOrderByCreatedAtDesc(feeRecord)
                .stream()
                .map(payment -> toResponse(payment, feeRecord))
                .toList();
    }

    // Find my payments
    @Transactional(readOnly = true)
    public List<PaymentResponse> findMyPayments() {
        User currentUser = getCurrentUser();

        return paymentRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(payment -> toResponse(payment, payment.getFeeRecord()))
                .toList();
    }

    // Find by centerId
    @Transactional(readOnly = true)
    public List<PaymentResponse> findAllByCenter() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        return paymentRepository.findAllByCenter_IdOrderByCreatedAtDesc(centerId)
                .stream()
                .map(payment -> toResponse(payment, payment.getFeeRecord()))
                .toList();
    }

    // Helper

    // Generate receipt number: RCP-YYYYMMDD-NNNNNN
    private String generateReceiptNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RCP-" + today + "-";
        String maxNumber = paymentRepository.findMaxReceiptNumberByPrefix(prefix);
        int nextSeq = 1;
        if (maxNumber != null && maxNumber.startsWith(prefix)) {
            try {
                nextSeq = Integer.parseInt(maxNumber.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                // fall through to default 1
            }
        }
        return prefix + String.format("%06d", nextSeq);
    }

    // To response
    private PaymentResponse toResponse(Payment payment, FeeRecord feeRecord) {

        BigDecimal remaining = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        String centerName = payment.getCenter() != null ? payment.getCenter().getName() : null;
        String className = feeRecord.getClazz() != null ? feeRecord.getClazz().getName() : null;
        String courseName = (feeRecord.getClazz() != null && feeRecord.getClazz().getCourse() != null)
                ? feeRecord.getClazz().getCourse().getName() : null;
        String collectedByUserName = payment.getCollectedByUser() != null
                ? payment.getCollectedByUser().getFullName() : null;

        return PaymentResponse.builder()
                .id(payment.getId())
                .receiptNumber(payment.getReceiptNumber())
                .feeRecordId(feeRecord.getId())
                .centerId(feeRecord.getCenter().getId())
                .centerName(centerName)
                .classId(feeRecord.getClazz().getId())
                .className(className)
                .courseName(courseName)
                .studentUserId(feeRecord.getStudentUser().getId())
                .studentPhoneNumber(feeRecord.getStudentUser().getPhoneNumber())
                .studentFullName(feeRecord.getStudentUser().getFullName())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .sepayRef(payment.getSepayRef())
                .note(payment.getNote())
                .collectedByUserId(payment.getCollectedByUser() != null ? payment.getCollectedByUser().getId() : null)
                .collectedByUserName(collectedByUserName)
                .createdAt(payment.getCreatedAt())
                .feeRecordAmount(feeRecord.getAmount())
                .feeRecordPaidAmount(feeRecord.getPaidAmount())
                .feeRecordRemainingAmount(remaining)
                .feeRecordStatus(feeRecord.getStatus())
                .build();
    }

    private FeeStatus resloveFeeStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        int compare = paidAmount.compareTo(BigDecimal.ZERO);

        if (compare <= 0) {
            return FeeStatus.UNPAID;
        }

        if (paidAmount.compareTo(totalAmount) >= 0) {
            return FeeStatus.PAID;
        }

        return FeeStatus.PARTIAL;
    }

    // Validate amount
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount must be greater than 0");
        }
    }

    // Normalize text
    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Get current user
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

    // Required current centerId
    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();

        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    // Assert can collect payment — only CASHIER is allowed to modify payment data
    private void assertCanCollectPayment(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only CASHIER can collect payment");
        }

        assertCenterMembership(currentUser, centerId);
    }

    // Assert center membership
    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);

        if (!hasMembership) {
            throw new AccessDeniedException("User is not member of this center");
        }
    }

    // Assert can view payments — OWNER or CASHIER
    private void assertCanViewPayments(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can view payments");
        }
        assertCenterMembership(currentUser, centerId);
    }

    // Audit log helper
    private void auditLog(User user, Center center, String action, String entityType, Long entityId, String description) {
        AuditLog logEntry = AuditLog.builder()
                .center(center)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(logEntry);
    }

    // ── Financial Timeline ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getFinancialTimeline(Long studentId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCanViewPayments(currentUser, centerId);

        List<TimelineEntryResponse> timeline = new ArrayList<>();

        // Payments
        paymentRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(studentId)
                .forEach(p -> timeline.add(TimelineEntryResponse.builder()
                        .timestamp(p.getCreatedAt())
                        .action(p.getStatus() == TransactionStatus.VOIDED ? "PAYMENT_VOIDED" : "PAYMENT_COLLECTED")
                        .userName(p.getCollectedByUser() != null ? p.getCollectedByUser().getFullName() : null)
                        .description(p.getStatus() == TransactionStatus.VOIDED ? "Voided: " + p.getVoidReason() : "Payment collected")
                        .amount(p.getAmount())
                        .entityId(p.getId())
                        .receiptNumber(p.getReceiptNumber())
                        .build()));

        // Refunds
        paymentRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(studentId)
                .forEach(p -> refundRepository.findAllByPaymentOrderByCreatedAtDesc(p)
                        .forEach(r -> timeline.add(TimelineEntryResponse.builder()
                                .timestamp(r.getCreatedAt())
                                .action("REFUND")
                                .userName(r.getCreatedBy().getFullName())
                                .description("Refund: " + (r.getReason() != null ? r.getReason() : ""))
                                .amount(r.getAmount())
                                .entityId(r.getId())
                                .build())));

        // Discounts — find via fee records for this student
        feeRecordRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(studentId)
                .forEach(fr -> discountRepository.findAllByFeeRecord_Id(fr.getId())
                        .forEach(d -> timeline.add(TimelineEntryResponse.builder()
                                .timestamp(d.getCreatedAt())
                                .action("DISCOUNT_APPLIED")
                                .userName(d.getCreatedBy().getFullName())
                                .description("Discount: " + d.getName() + " (" + d.getType() + " " + d.getValue() + ")")
                                .amount(null)
                                .entityId(d.getId())
                                .build())));

        // Sort by timestamp descending
        timeline.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return timeline;
    }
}
