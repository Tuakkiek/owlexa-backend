package com.owlexa.owlexabackend.modules.payment.service;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.BankTransferQrResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentHistoryResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.TimelineEntryResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.DeviceTypeConverter;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.teacher.entity.BulkTeacherStatus;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.document.entity.DocumentType;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.AuditLog;
import com.owlexa.owlexabackend.modules.payment.entity.Installment;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import com.owlexa.owlexabackend.modules.payment.entity.Refund;
import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.payment.repository.SePayWebhookEventRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    public static final String DUPLICATE_PAYMENT_CODE = "DUPLICATE_PAYMENT";

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final InstallmentRepository installmentRepository;
    private final RefundRepository refundRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final BankTransferQrService bankTransferQrService;
    private final SePayWebhookEventRepository sePayWebhookEventRepository;

    @Value("${app.payment.bank-transfer-expiry-minutes:30}")
    private int bankTransferExpiryMinutes;

    @Value("${sepay.payment-code.prefix:OWX}")
    private String paymentCodePrefix;


    // ── Create pending bank transfer payment ──────────────────────────────

    @Transactional
    public PaymentResponse createPendingBankTransfer(Long feeRecordId, CashPaymentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);
        assertCanCollectPayment(currentUser, centerId);

        FeeRecord feeRecord = paymentRepository.findFeeRecordByIdForUpdate(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if (!feeRecord.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Fee record " + feeRecordId + " belongs to another center");
        }

        validateAmount(request.getAmount());

        BigDecimal remainingAmount = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BusinessRuleException("Số tiền thanh toán vượt quá dư nợ còn lại");
        }

        Instant now = Instant.now();
        var existingPending = findValidPendingPayment(feeRecordId, feeRecord.getStudentUser().getId(), now);
        if (existingPending.isPresent()) {
            Payment pending = existingPending.get();
            log.debug("Returning existing pending bank transfer {} for feeRecord {}", pending.getId(), feeRecordId);
            return toResponse(pending, feeRecord);
        }

        expireStalePendingPayments(feeRecordId, feeRecord.getStudentUser().getId(), now);

        PaymentMethod method = request.getMethod() != null ? request.getMethod() : PaymentMethod.SEPAY;
        String receiptNumber = generateReceiptNumber();
        Instant expiresAt = now.plusSeconds((long) bankTransferExpiryMinutes * 60);

        Payment payment = Payment.builder()
                .receiptNumber(receiptNumber)
                .feeRecord(feeRecord)
                .center(feeRecord.getCenter())
                .studentUser(feeRecord.getStudentUser())
                .collectedByUser(currentUser)
                .amount(request.getAmount())
                .method(method)
                .note(normalizeText(request.getNote()))
                .status(TransactionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        payment = paymentRepository.save(payment);

        // Generate payment code using fixed-length format: OWX000001
        String paymentCode = generatePaymentCode(payment.getId());
        payment.setSepayRef(paymentCode);
        payment = paymentRepository.save(payment);

        // Do NOT update FeeRecord here — only webhook confirmation updates FeeRecord
        // Do NOT allocate installments — only webhook confirmation does that

        // Audit log
        auditLog(currentUser, feeRecord.getCenter(), "PAYMENT_PENDING", "Payment",
                payment.getId(), "Created pending bank transfer for " + request.getAmount()
                        + " VND from " + feeRecord.getStudentUser().getFullName()
                        + " - Code: " + paymentCode + " - Expires: " + expiresAt);

        return toResponse(payment, feeRecord);
    }

    // ── Student QR Payment (always full remaining balance) ─────────────────

    /**
     * Creates a PENDING bank-transfer payment for the FULL remaining balance
     * of a fee record. Only callable by the student who owns the fee record.
     * <p>
     * Business rule: Student self-service QR payments MUST always cover the
     * entire remaining balance in one transaction. No partial amounts, no
     * custom amounts — the backend is the single source of truth.
     * <p>
     * <b>Race-condition safety:</b> Uses pessimistic write lock on the FeeRecord
     * to guarantee only one pending payment can exist at a time. If a valid
     * pending payment already exists, it is returned instead of creating a new one.
     * Only creates a new payment when:
     * <ul>
     *   <li>there is no pending payment, OR</li>
     *   <li>the previous pending payment has expired</li>
     * </ul>
     * <p>
     * <b>Idempotency:</b> Accepts an optional idempotency key. If a payment
     * with the same key already exists, returns it without creating a duplicate.
     *
     * @param feeRecordId     the fee record to pay
     * @param idempotencyKey  optional client-generated UUID for idempotency
     * @return the existing or newly created pending payment
     */
    @Transactional
    public PaymentResponse createStudentPendingBankTransfer(Long feeRecordId, String idempotencyKey) {
        User currentUser = getCurrentUser();

        // ── Idempotency guard: if a request with this key already succeeded, return it ──
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingByIdempotency = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingByIdempotency.isPresent()) {
                Payment existing = existingByIdempotency.get();
                // Verify ownership
                if (!existing.getStudentUser().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Idempotency key belongs to another student's payment");
                }
                log.debug("Idempotency key {} already processed — returning existing payment {}",
                        idempotencyKey, existing.getId());
                return toResponse(existing, existing.getFeeRecord());
            }
        }

        // ── Pessimistic lock on FeeRecord to prevent concurrent payment creation ──
        FeeRecord feeRecord = paymentRepository.findFeeRecordByIdForUpdate(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        // Verify the fee record belongs to the authenticated student
        if (!feeRecord.getStudentUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only pay your own fee records");
        }

        boolean activeEnrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(feeRecord.getClazz().getId(), currentUser.getId())
                .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .orElse(false);

        if (!activeEnrollment) {
            throw new BusinessRuleException("Không thể đóng học phí cho lớp học mà bạn không còn tham gia");
        }

        BigDecimal remainingAmount = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Hóa đơn này đã được thanh toán đầy đủ");
        }

        // ── Check for existing valid pending payment (the core duplicate-prevention logic) ──
        Instant now = Instant.now();
        var existingPending = findValidPendingPayment(feeRecordId, currentUser.getId(), now);

        if (existingPending.isPresent()) {
            Payment pending = existingPending.get();

            // If the existing pending payment is NOT expired, return it — do NOT create another
            if (pending.getExpiresAt() == null || now.isBefore(pending.getExpiresAt())) {
                log.debug("Returning existing valid pending payment {} for feeRecord {}",
                        pending.getId(), feeRecordId);
                return toResponse(pending, feeRecord);
            }

            // Existing pending is expired — mark it as EXPIRED and create a new one
            pending.setStatus(TransactionStatus.EXPIRED);
            pending.setVoidReason("Expired — superseded by new payment");
            pending.setVoidedAt(Instant.now());
            paymentRepository.save(pending);
            log.debug("Expired pending payment {} for feeRecord {}", pending.getId(), feeRecordId);
        }

        // ── Create new PENDING payment for the FULL remaining balance ──
        expireStalePendingPayments(feeRecordId, currentUser.getId(), now);

        PaymentMethod method = PaymentMethod.SEPAY;
        String receiptNumber = generateReceiptNumber();
        Instant expiresAt = now.plusSeconds((long) bankTransferExpiryMinutes * 60);

        Payment payment = Payment.builder()
                .receiptNumber(receiptNumber)
                .feeRecord(feeRecord)
                .center(feeRecord.getCenter())
                .studentUser(feeRecord.getStudentUser())
                .collectedByUser(currentUser)
                .amount(remainingAmount)
                .method(method)
                .note("Student QR payment — full remaining balance")
                .status(TransactionStatus.PENDING)
                .expiresAt(expiresAt)
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null)
                .build();

        payment = paymentRepository.save(payment);

        String paymentCode = generatePaymentCode(payment.getId());
        payment.setSepayRef(paymentCode);
        payment = paymentRepository.save(payment);

        auditLog(currentUser, feeRecord.getCenter(), "PAYMENT_PENDING", "Payment",
                payment.getId(), "Student created QR payment for full remaining balance: "
                        + remainingAmount + " VND - Code: " + paymentCode
                        + " - Expires: " + expiresAt
                        + (idempotencyKey != null ? " - IdempotencyKey: " + idempotencyKey : ""));

        return toResponse(payment, feeRecord);
    }

    // ── Get current pending payment for a fee record (no side effects) ─────

    /**
     * Returns the currently active pending payment for a fee record + student.
     * Does NOT create anything. Used by the frontend to check if there's an
     * unfinished payment to resume.
     *
     * @param feeRecordId the fee record
     * @return the pending payment wrapped in Optional, or Optional.empty() if none exists
     */
    @Transactional(readOnly = true)
    public Optional<PaymentResponse> getCurrentPendingPayment(Long feeRecordId) {
        User currentUser = getCurrentUser();

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if (!feeRecord.getStudentUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view your own payments");
        }

        var pending = findValidPendingPayment(feeRecordId, currentUser.getId(), Instant.now());

        return pending.map(p -> toResponse(p, feeRecord));
    }

    // ── Cancel student's own pending payment ─────────────────────────────

    /**
     * Allows a student to cancel their own pending payment.
     * Only cancellable while status == PENDING.
     * After cancellation: status becomes VOIDED.
     *
     * @param paymentId the payment to cancel
     * @return the cancelled payment response
     */
    @Transactional
    public PaymentResponse cancelStudentPendingPayment(Long paymentId) {
        User currentUser = getCurrentUser();

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        // Only the student who owns the payment can cancel it
        if (!payment.getStudentUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only cancel your own payments");
        }

        // Only PENDING payments can be cancelled
        if (payment.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessRuleException(
                    "Chỉ có thể hủy các thanh toán đang chờ xử lý. Trạng thái hiện tại: " + payment.getStatus());
        }

        payment.setStatus(TransactionStatus.VOIDED);
        payment.setVoidReason("Cancelled by student");
        payment.setVoidedBy(currentUser);
        payment.setVoidedAt(Instant.now());
        paymentRepository.save(payment);

        auditLog(currentUser, payment.getCenter(), "PAYMENT_CANCELLED", "Payment",
                payment.getId(), "Student cancelled pending payment " + payment.getReceiptNumber()
                        + " - Code: " + payment.getSepayRef());

        return toResponse(payment, payment.getFeeRecord());
    }

    // ── Student QR view (validate ownership) ──────────────────────────────

    /**
     * Returns QR data for a payment, validating that the authenticated student
     * owns the payment. This is the student-specific companion to the
     * cashier/owner QR endpoint.
     */
    @Transactional(readOnly = true)
    public BankTransferQrResponse getStudentPaymentQr(Long paymentId) {
        User currentUser = getCurrentUser();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với ID: " + paymentId));

        if (!payment.getStudentUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view QR codes for your own payments");
        }

        if (payment.getStatus() != TransactionStatus.PENDING
                && payment.getStatus() != TransactionStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Mã QR chỉ có sẵn cho các thanh toán chuyển khoản đang chờ hoặc đã xác nhận");
        }

        return bankTransferQrService.buildQrResponse(payment);
    }

    /**
     * Generates a fixed-length, human-readable payment code.
     * Format: {prefix}{id:06d}  e.g., OWX000001, OWX000015
     * The prefix is configurable via sepay.payment-code.prefix.
     */
    private String generatePaymentCode(Long paymentId) {
        return paymentCodePrefix + String.format("%06d", paymentId);
    }

    // ── Confirm pending bank transfer payment (called by SePay webhook) ───

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public PaymentResponse confirmBankTransferPayment(Long paymentId, SePayWebhookRequest webhookRequest) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        // ── TEMPORARY DEBUG LOGGING (remove for production) ──
        TransactionStatus statusBefore = payment.getStatus();
        log.debug("[SEPAY-WEBHOOK] Payment id={}: status BEFORE = {}, expiresAt = {}",
                paymentId, statusBefore, payment.getExpiresAt());
        // ── END TEMPORARY DEBUG ──

        if (payment.getStatus() == TransactionStatus.ACTIVE) {
            throw new BusinessRuleException(DUPLICATE_PAYMENT_CODE,
                    "DUPLICATE_PAYMENT: payment " + paymentId + " was already confirmed");
        }
        if (payment.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessRuleException("Không thể xác nhận giao dịch đã bị hủy");
        }
        if (payment.getStatus() == TransactionStatus.EXPIRED) {
            throw new BusinessRuleException("Không thể xác nhận giao dịch đã hết hạn");
        }
        if (payment.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessRuleException("Giao dịch thanh toán " + paymentId + " không ở trạng thái CHỜ XỬ LÝ");
        }

        // Guard against confirming payments past their expiration time
        if (payment.getExpiresAt() != null && Instant.now().isAfter(payment.getExpiresAt())) {
            throw new BusinessRuleException("Giao dịch thanh toán " + paymentId + " đã hết hạn lúc " + payment.getExpiresAt());
        }

        // Update payment status — preserve original sepayRef (payment code)
        FeeRecord feeRecord = paymentRepository.findFeeRecordByIdForUpdate(payment.getFeeRecord().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + payment.getFeeRecord().getId()));

        BigDecimal remainingAmount = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            markRejectedDuplicatePayment(payment,
                    "DUPLICATE_PAYMENT: fee record already fully paid",
                    DUPLICATE_PAYMENT_CODE);
            throw new BusinessRuleException(DUPLICATE_PAYMENT_CODE,
                    "DUPLICATE_PAYMENT: fee record already fully paid");
        }
        if (payment.getAmount().compareTo(remainingAmount) > 0) {
            markRejectedDuplicatePayment(payment,
                    "DUPLICATE_PAYMENT: amount exceeds remaining balance",
                    DUPLICATE_PAYMENT_CODE);
            throw new BusinessRuleException(DUPLICATE_PAYMENT_CODE,
                    "DUPLICATE_PAYMENT: amount exceeds remaining balance");
        }

        payment.setStatus(TransactionStatus.ACTIVE);
        // Store SePay's reference in the note for reconciliation, don't overwrite payment code
        if (webhookRequest.getReferenceCode() != null && !webhookRequest.getReferenceCode().isBlank()) {
            String existingNote = payment.getNote();
            String sepayNote = " [SePay: " + webhookRequest.getReferenceCode() + "]";
            payment.setNote((existingNote != null ? existingNote : "") + sepayNote);
        }
        paymentRepository.save(payment);

        // ── TEMPORARY DEBUG LOGGING (remove for production) ──
        log.debug("[SEPAY-WEBHOOK] Payment id={}: status AFTER = {}, sepayRef = {}",
                paymentId, payment.getStatus(), payment.getSepayRef());
        // ── END TEMPORARY DEBUG ──

        // Allocate to oldest unpaid installment if installments exist
        allocateToInstallments(feeRecord, payment.getAmount());

        BigDecimal newPaidAmount = feeRecord.getPaidAmount().add(payment.getAmount());
        feeRecord.setPaidAmount(newPaidAmount);
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount(), newPaidAmount));
        feeRecordRepository.save(feeRecord);

        reactivateSuspendedEnrollmentIfTuitionCleared(feeRecord, payment.getCollectedByUser());

        // Audit log
        auditLog(payment.getCollectedByUser(), payment.getCenter(), "PAYMENT_CONFIRMED", "Payment",
                payment.getId(), "Bank transfer confirmed via SePay for " + payment.getAmount()
                        + " VND from " + feeRecord.getStudentUser().getFullName()
                        + " - Receipt: " + payment.getReceiptNumber()
                        + " - SePay ref: " + webhookRequest.getReferenceCode());

        return toResponse(payment, feeRecord);
    }

    // Collect cash
    @Transactional
    public PaymentResponse collectCash(Long feeRecordId, CashPaymentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);
        assertCanCollectPayment(currentUser, centerId);

        FeeRecord feeRecord = paymentRepository.findFeeRecordByIdForUpdate(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if(!feeRecord.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Fee record " + feeRecordId + " belongs to another center");
        }

        validateAmount(request.getAmount());

        BigDecimal remainingAmount = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        if(request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BusinessRuleException("Số tiền thanh toán vượt quá dư nợ còn lại");
        }

        Instant now = Instant.now();
        expireStalePendingPayments(feeRecordId, feeRecord.getStudentUser().getId(), now);
        if (findValidPendingPayment(feeRecordId, feeRecord.getStudentUser().getId(), now).isPresent()) {
            throw new BusinessRuleException(
                    "Hóa đơn này đã có một khoản thanh toán chờ xử lý. Vui lòng chờ xử lý hoặc hủy khoản thanh toán trước khi thu tiền mặt.");
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
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount(), newPaidAmount));

        feeRecordRepository.save(feeRecord);

        // If payment clears overdue condition, reactivate any SUSPENDED enrollment
        // for this student+class (per business rule: payment → ACTIVE again).
        reactivateSuspendedEnrollmentIfTuitionCleared(feeRecord, currentUser);

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
                                                   TransactionStatus status,
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
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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

    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> findPaymentHistoryPaginated(Long centerId,
                                                                    String studentQuery,
                                                                    Long cashierId,
                                                                    PaymentMethod method,
                                                                    String status,
                                                                    Instant startDate,
                                                                    Instant endDate,
                                                                    Pageable pageable) {
        User currentUser = getCurrentUser();
        assertCenterMembership(currentUser, centerId);

        List<PaymentHistoryResponse> items = findPaymentHistoryItems(
                centerId, studentQuery, cashierId, method, status, startDate, endDate);
        return paginatePaymentHistory(items, pageable);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> findMyPaymentHistory() {
        User currentUser = getCurrentUser();
        List<PaymentHistoryResponse> items = new ArrayList<>();

        paymentRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .filter(payment -> isVisibleToStudent(payment, currentUser.getId()))
                .map(this::toHistoryResponse)
                .forEach(items::add);

        sePayWebhookEventRepository.findDuplicatePaymentRowsByStudentUserId(currentUser.getId())
                .stream()
                .map(this::toDuplicateHistoryResponse)
                .forEach(items::add);

        items.sort(Comparator.comparing(PaymentHistoryResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return items;
    }

    @Transactional(readOnly = true)
    public com.owlexa.owlexabackend.modules.payment.dto.response.PaymentSummaryResponse getPaymentHistorySummary(
            Long centerId,
            String studentQuery,
            Long cashierId,
            PaymentMethod method,
            String status,
            Instant startDate,
            Instant endDate) {

        User currentUser = getCurrentUser();
        assertCenterMembership(currentUser, centerId);

        List<PaymentHistoryResponse> items = findPaymentHistoryItems(
                centerId, studentQuery, cashierId, method, status, startDate, endDate);

        BigDecimal totalRevenue = items.stream()
                .filter(item -> "PAYMENT".equals(item.getSource()))
                .filter(item -> TransactionStatus.ACTIVE.name().equals(item.getStatus()))
                .map(PaymentHistoryResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = items.stream()
                .filter(item -> TransactionStatus.PENDING.name().equals(item.getStatus()))
                .count();

        return com.owlexa.owlexabackend.modules.payment.dto.response.PaymentSummaryResponse.builder()
                .totalTransactions(items.size())
                .totalRevenue(totalRevenue)
                .pendingCount(pendingCount)
                .build();
    }

    // ── Payment Summary ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public com.owlexa.owlexabackend.modules.payment.dto.response.PaymentSummaryResponse getPaymentSummary(
            Long centerId,
            String studentQuery,
            Long cashierId,
            PaymentMethod method,
            TransactionStatus status,
            Instant startDate,
            Instant endDate) {

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
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Payment> payments = paymentRepository.findAll(spec);

        long totalTransactions = payments.size();

        BigDecimal totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == TransactionStatus.ACTIVE)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = payments.stream()
                .filter(p -> p.getStatus() == TransactionStatus.PENDING)
                .count();

        return com.owlexa.owlexabackend.modules.payment.dto.response.PaymentSummaryResponse.builder()
                .totalTransactions(totalTransactions)
                .totalRevenue(totalRevenue)
                .pendingCount(pendingCount)
                .build();
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với ID: " + paymentId));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment belongs to another center");
        }
        if (payment.getStatus() == TransactionStatus.VOIDED) {
            throw new BusinessRuleException("Giao dịch thanh toán đã bị hủy trước đó");
        }
        if (payment.getStatus() == TransactionStatus.EXPIRED) {
            throw new BusinessRuleException("Không thể hủy giao dịch đã hết hạn");
        }

        boolean wasActive = payment.getStatus() == TransactionStatus.ACTIVE;

        payment.setStatus(TransactionStatus.VOIDED);
        payment.setVoidReason(normalizeText(reason));
        payment.setVoidedBy(currentUser);
        payment.setVoidedAt(Instant.now());
        paymentRepository.save(payment);

        // Only reverse paidAmount if the payment was ACTIVE (had actually updated FeeRecord).
        // PENDING and other non-ACTIVE payments never incremented paidAmount.
        if (wasActive) {
            FeeRecord feeRecord = payment.getFeeRecord();
            feeRecord.setPaidAmount(feeRecord.getPaidAmount().subtract(payment.getAmount()));
            feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount(), feeRecord.getPaidAmount()));
            feeRecordRepository.save(feeRecord);

            auditLog(currentUser, payment.getCenter(), "PAYMENT_VOIDED", "Payment",
                    payment.getId(), "Voided payment " + payment.getReceiptNumber() + " - Reason: " + reason);
        } else {
            // PENDING payment voided — FeeRecord was never updated, no reversal needed
            auditLog(currentUser, payment.getCenter(), "PAYMENT_VOIDED", "Payment",
                    payment.getId(), "Cancelled pending payment " + payment.getReceiptNumber() + " - Reason: " + reason);
        }

        return toResponse(payment, payment.getFeeRecord());
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với ID: " + paymentId));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment belongs to another center");
        }
        if (payment.getStatus() != TransactionStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ các giao dịch thanh toán đang hoạt động mới có thể hoàn tiền");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền hoàn trả phải lớn hơn 0");
        }

        BigDecimal alreadyRefunded = refundRepository.findAllByPaymentOrderByCreatedAtDesc(payment)
                .stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);
        if (amount.compareTo(maxRefundable) > 0) {
            throw new BusinessRuleException("Số tiền hoàn trả vượt quá số dư khả dụng");
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
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount(), feeRecord.getPaidAmount()));
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
                .filter(payment -> {
                    if (payment.getFeeRecord() == null || payment.getFeeRecord().getClazz() == null) {
                        return true;
                    }
                    return classEnrollmentRepository
                            .findByClazz_IdAndStudentUser_Id(
                                    payment.getFeeRecord().getClazz().getId(),
                                    currentUser.getId())
                            .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                            .orElse(false);
                })
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

    private Optional<Payment> findValidPendingPayment(Long feeRecordId, Long studentUserId, Instant now) {
        return paymentRepository.findValidPendingByFeeRecordAndStudent(feeRecordId, studentUserId, now)
                .stream()
                .findFirst();
    }

    private void expireStalePendingPayments(Long feeRecordId, Long studentUserId, Instant now) {
        paymentRepository.findExpiredPendingByFeeRecordAndStudent(feeRecordId, studentUserId, now)
                .forEach(payment -> {
                    payment.setStatus(TransactionStatus.EXPIRED);
                    payment.setVoidReason("Expired - superseded by new payment");
                    payment.setVoidedAt(now);
                    paymentRepository.save(payment);
                    log.debug("Expired pending payment {} for feeRecord {}", payment.getId(), feeRecordId);
                });
    }

    private void markRejectedDuplicatePayment(Payment payment, String reason, String auditAction) {
        payment.setStatus(TransactionStatus.VOIDED);
        payment.setVoidReason(reason);
        payment.setVoidedAt(Instant.now());
        paymentRepository.save(payment);
        auditLog(payment.getCollectedByUser(), payment.getCenter(), auditAction, "Payment",
                payment.getId(), reason);
    }

    private List<PaymentHistoryResponse> findPaymentHistoryItems(Long centerId,
                                                                 String studentQuery,
                                                                 Long cashierId,
                                                                 PaymentMethod method,
                                                                 String status,
                                                                 Instant startDate,
                                                                 Instant endDate) {
        boolean duplicateOnly = DUPLICATE_PAYMENT_CODE.equals(status);
        TransactionStatus paymentStatus = parsePaymentStatus(status);
        List<PaymentHistoryResponse> items = new ArrayList<>();

        if (!duplicateOnly) {
            paymentRepository.findAll(buildPaymentSpecification(
                            centerId, studentQuery, cashierId, method, paymentStatus, startDate, endDate))
                    .stream()
                    .map(this::toHistoryResponse)
                    .forEach(items::add);
        }

        if (status == null || status.isBlank() || duplicateOnly) {
            sePayWebhookEventRepository.findDuplicatePaymentRowsByCenterId(centerId)
                    .stream()
                    .filter(row -> duplicateEventMatchesFilters(row, studentQuery, cashierId, method, startDate, endDate))
                    .map(this::toDuplicateHistoryResponse)
                    .forEach(items::add);
        }

        items.sort(Comparator.comparing(PaymentHistoryResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return items;
    }

    private Specification<Payment> buildPaymentSpecification(Long centerId,
                                                             String studentQuery,
                                                             Long cashierId,
                                                             PaymentMethod method,
                                                             TransactionStatus status,
                                                             Instant startDate,
                                                             Instant endDate) {
        return (root, query, cb) -> {
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
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionStatus parsePaymentStatus(String status) {
        if (status == null || status.isBlank() || DUPLICATE_PAYMENT_CODE.equals(status)) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported payment status filter: " + status);
        }
    }

    private boolean duplicateEventMatchesFilters(Object[] row,
                                                 String studentQuery,
                                                 Long cashierId,
                                                 PaymentMethod method,
                                                 Instant startDate,
                                                 Instant endDate) {
        SePayWebhookEvent event = (SePayWebhookEvent) row[0];
        Payment payment = (Payment) row[1];
        Instant eventTime = sePayEventInstant(event);

        if (cashierId != null && (payment.getCollectedByUser() == null
                || !cashierId.equals(payment.getCollectedByUser().getId()))) {
            return false;
        }
        if (method != null && payment.getMethod() != method) {
            return false;
        }
        if (startDate != null && (eventTime == null || eventTime.isBefore(startDate))) {
            return false;
        }
        if (endDate != null && (eventTime == null || !eventTime.isBefore(endDate))) {
            return false;
        }
        if (studentQuery != null && !studentQuery.isBlank()) {
            String query = studentQuery.toLowerCase();
            User student = payment.getStudentUser();
            String name = student != null && student.getFullName() != null
                    ? student.getFullName().toLowerCase()
                    : "";
            String phone = student != null && student.getPhoneNumber() != null
                    ? student.getPhoneNumber().toLowerCase()
                    : "";
            return name.contains(query) || phone.contains(query);
        }
        return true;
    }

    private Page<PaymentHistoryResponse> paginatePaymentHistory(List<PaymentHistoryResponse> items, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    private PaymentHistoryResponse toHistoryResponse(Payment payment) {
        PaymentResponse response = toResponse(payment, payment.getFeeRecord());
        return PaymentHistoryResponse.builder()
                .id("payment-" + response.getId())
                .paymentId(response.getId())
                .source("PAYMENT")
                .receiptNumber(response.getReceiptNumber())
                .feeRecordId(response.getFeeRecordId())
                .centerId(response.getCenterId())
                .classId(response.getClassId())
                .className(response.getClassName())
                .courseName(response.getCourseName())
                .studentUserId(response.getStudentUserId())
                .studentPhoneNumber(response.getStudentPhoneNumber())
                .studentFullName(response.getStudentFullName())
                .amount(response.getAmount())
                .method(response.getMethod())
                .sepayRef(response.getSepayRef())
                .note(response.getNote())
                .collectedByUserId(response.getCollectedByUserId())
                .collectedByUserName(response.getCollectedByUserName())
                .centerName(response.getCenterName())
                .status(response.getStatus().name())
                .createdAt(response.getCreatedAt())
                .expiresAt(response.getExpiresAt())
                .feeRecordAmount(response.getFeeRecordAmount())
                .feeRecordPaidAmount(response.getFeeRecordPaidAmount())
                .feeRecordRemainingAmount(response.getFeeRecordRemainingAmount())
                .feeRecordStatus(response.getFeeRecordStatus())
                .build();
    }

    private PaymentHistoryResponse toDuplicateHistoryResponse(Object[] row) {
        SePayWebhookEvent event = (SePayWebhookEvent) row[0];
        Payment payment = (Payment) row[1];
        FeeRecord feeRecord = payment.getFeeRecord();
        User student = payment.getStudentUser();
        Center center = payment.getCenter();
        Class clazz = feeRecord != null ? feeRecord.getClazz() : null;
        Instant eventTime = sePayEventInstant(event);

        BigDecimal duplicateAmount = event.getTransferAmount() != null
                ? BigDecimal.valueOf(event.getTransferAmount())
                : payment.getAmount();

        return PaymentHistoryResponse.builder()
                .id("sepay-event-" + (event.getId() != null ? event.getId() : event.getSepayTransactionId()))
                .paymentId(payment.getId())
                .webhookEventId(event.getId())
                .source("SEPAY_EVENT")
                .receiptNumber("DUP-" + event.getSepayTransactionId())
                .feeRecordId(feeRecord != null ? feeRecord.getId() : null)
                .centerId(center != null ? center.getId() : null)
                .centerName(center != null ? center.getName() : null)
                .classId(clazz != null ? clazz.getId() : null)
                .className(clazz != null ? clazz.getName() : null)
                .courseName(clazz != null && clazz.getCourse() != null ? clazz.getCourse().getName() : null)
                .studentUserId(student != null ? student.getId() : null)
                .studentPhoneNumber(student != null ? student.getPhoneNumber() : null)
                .studentFullName(student != null ? student.getFullName() : null)
                .amount(duplicateAmount)
                .method(payment.getMethod())
                .sepayRef(event.getReferenceCode() != null ? event.getReferenceCode() : event.getPaymentCode())
                .note(event.getProcessingNote() != null ? event.getProcessingNote() : event.getContent())
                .collectedByUserId(payment.getCollectedByUser() != null ? payment.getCollectedByUser().getId() : null)
                .collectedByUserName(payment.getCollectedByUser() != null ? payment.getCollectedByUser().getFullName() : null)
                .status(DUPLICATE_PAYMENT_CODE)
                .createdAt(eventTime)
                .feeRecordAmount(feeRecord != null ? feeRecord.getAmount() : null)
                .feeRecordPaidAmount(feeRecord != null ? feeRecord.getPaidAmount() : null)
                .feeRecordRemainingAmount(feeRecord != null
                        ? feeRecord.getAmount().subtract(feeRecord.getPaidAmount())
                        : null)
                .feeRecordStatus(feeRecord != null ? feeRecord.getStatus() : null)
                .build();
    }

    private Instant sePayEventInstant(SePayWebhookEvent event) {
        if (event.getProcessedAt() != null) {
            return event.getProcessedAt().atZone(ZoneId.systemDefault()).toInstant();
        }
        if (event.getReceivedAt() != null) {
            return event.getReceivedAt().atZone(ZoneId.systemDefault()).toInstant();
        }
        return null;
    }

    private boolean isVisibleToStudent(Payment payment, Long studentUserId) {
        if (payment.getFeeRecord() == null || payment.getFeeRecord().getClazz() == null) {
            return true;
        }
        return classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(
                        payment.getFeeRecord().getClazz().getId(),
                        studentUserId)
                .map(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .orElse(false);
    }

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
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .expiresAt(payment.getExpiresAt())
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

    private void reactivateSuspendedEnrollmentIfTuitionCleared(FeeRecord feeRecord, User actor) {
        if (feeRecord.getClazz() == null || feeRecord.getStudentUser() == null) {
            return;
        }
        if (feeRecord.getStatus() != FeeStatus.PAID) {
            return;
        }

        long outstandingDueCount = feeRecordRepository.countOutstandingDueByStudentAndClass(
                feeRecord.getStudentUser().getId(),
                feeRecord.getClazz().getId(),
                List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL, FeeStatus.OVERDUE),
                LocalDate.now());

        if (outstandingDueCount > 0) {
            return;
        }

        classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(
                        feeRecord.getClazz().getId(),
                        feeRecord.getStudentUser().getId())
                .ifPresent(enrollment -> {
                    if (enrollment.getStatus() == EnrollmentStatus.SUSPENDED) {
                        enrollment.setStatus(EnrollmentStatus.ACTIVE);
                        classEnrollmentRepository.save(enrollment);
                        auditLog(actor, feeRecord.getCenter(), "ENROLLMENT_REACTIVATED", "ClassEnrollment",
                                enrollment.getId(), "Auto reactivated after tuition was fully paid for class "
                                        + feeRecord.getClazz().getId());
                    }
                });
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

    // Assert can collect payment — OWNER and CASHIER can record payment data.
    private void assertCanCollectPayment(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can collect payment");
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

        // Payments — include all statuses in timeline
        paymentRepository.findAllByStudentUser_IdOrderByCreatedAtDesc(studentId)
                .forEach(p -> {
                    String action;
                    String description;
                    switch (p.getStatus()) {
                        case PENDING:
                            action = "PAYMENT_PENDING";
                            description = "Bank transfer pending - Code: " + p.getSepayRef();
                            break;
                        case EXPIRED:
                            action = "PAYMENT_EXPIRED";
                            description = "Bank transfer expired - Code: " + p.getSepayRef();
                            break;
                        case VOIDED:
                            action = "PAYMENT_VOIDED";
                            description = "Voided: " + p.getVoidReason();
                            break;
                        default: // ACTIVE
                            action = p.getMethod() == PaymentMethod.CASH
                                    ? "PAYMENT_COLLECTED"
                                    : "PAYMENT_CONFIRMED";
                            description = p.getMethod() == PaymentMethod.CASH
                                    ? "Payment collected"
                                    : "Bank transfer confirmed - Receipt: " + p.getReceiptNumber();
                            break;
                    }
                    timeline.add(TimelineEntryResponse.builder()
                            .timestamp(p.getCreatedAt())
                            .action(action)
                            .userName(p.getCollectedByUser() != null ? p.getCollectedByUser().getFullName() : null)
                            .description(description)
                            .amount(p.getAmount())
                            .entityId(p.getId())
                            .receiptNumber(p.getReceiptNumber())
                            .build());
                });

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

        // Sort by timestamp descending
        timeline.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return timeline;
    }
}
