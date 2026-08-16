package com.owlexa.owlexabackend.modules.payment.controller;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.BankTransferQrResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentSummaryResponse;
import com.owlexa.owlexabackend.modules.payment.dto.response.TimelineEntryResponse;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.service.BankTransferQrService;
import com.owlexa.owlexabackend.modules.payment.service.PaymentService;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BankTransferQrService bankTransferQrService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/cashier/fee-record/{feeRecordId}/payments/cash")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public PaymentResponse collectCash(
            @PathVariable Long feeRecordId,
            @Valid @RequestBody CashPaymentRequest request
    ) {
        return paymentService.collectCash(feeRecordId, request);
    }

    @PostMapping("/cashier/fee-record/{feeRecordId}/payments/bank-transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public PaymentResponse createBankTransfer(
            @PathVariable Long feeRecordId,
            @Valid @RequestBody CashPaymentRequest request
    ) {
        return paymentService.createPendingBankTransfer(feeRecordId, request);
    }

    @GetMapping({"/cashier/payments/{paymentId}/qr", "/owner/payments/{paymentId}/qr"})
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public BankTransferQrResponse getPaymentQr(@PathVariable Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId != null && !payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Payment " + paymentId + " belongs to another center");
        }

        if (payment.getStatus() != TransactionStatus.PENDING
                && payment.getStatus() != TransactionStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "QR is only available for pending or confirmed bank transfer payments");
        }

        return bankTransferQrService.buildQrResponse(payment);
    }

    // ── Student self-service QR endpoints ─────────────────────────────────

    /**
     * Creates a QR payment for the FULL remaining balance of a fee record.
     * The student cannot choose the amount — the backend always uses the
     * current remaining balance as the single source of truth.
     * <p>
     * Supports Idempotency-Key header for safe retries.
     * Uses pessimistic locking to prevent duplicate pending payments.
     */
    @PostMapping("/student/fee-record/{feeRecordId}/payments/qr")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createStudentQrPayment(
            @PathVariable Long feeRecordId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return paymentService.createStudentPendingBankTransfer(feeRecordId, idempotencyKey);
    }

    /**
     * Returns the current pending payment for a fee record (if any).
     * Used by the frontend to check for unfinished payments and resume them.
     * Returns 200 with the payment if one exists, or 204 No Content if none exists.
     * Returns 404 only when the fee record itself is not found or the student is not authorized.
     */
    @GetMapping("/student/fee-record/{feeRecordId}/payments/pending")
    public ResponseEntity<PaymentResponse> getCurrentPendingPayment(@PathVariable Long feeRecordId) {
        return paymentService.getCurrentPendingPayment(feeRecordId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Allows a student to cancel their own pending payment.
     * Only cancellable while status == PENDING.
     */
    @PostMapping("/student/payments/{paymentId}/cancel")
    public PaymentResponse cancelStudentPayment(@PathVariable Long paymentId) {
        return paymentService.cancelStudentPendingPayment(paymentId);
    }

    /**
     * Returns QR display data for a student-owned payment.
     * Validates that the authenticated student owns the payment.
     */
    @GetMapping("/student/payments/{paymentId}/qr")
    public BankTransferQrResponse getStudentPaymentQr(@PathVariable Long paymentId) {
        return paymentService.getStudentPaymentQr(paymentId);
    }

    @GetMapping({
            "/owner/fee-record/{feeRecordId}/payments",
            "/cashier/fee-record/{feeRecordId}/payments"
    })
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public List<PaymentResponse> findAllByFeeRecord(
            @PathVariable Long feeRecordId
    ) {
        return paymentService.findAllByFeeRecord(feeRecordId);
    }

    @GetMapping("/student/payments/me")
    public List<PaymentResponse> findMyPayment() {
        return paymentService.findMyPayments();
    }

    @GetMapping({
            "/owner/payments",
            "/cashier/payments"
    })
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public Page<PaymentResponse> findAllPaginated(
            @RequestParam(required = false) String student,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Long centerId = TenantContext.getCurrentTenantId();
        return paymentService.findAllPaginated(centerId, student, cashierId, method, status, startDate, endDate, pageable);
    }

    @GetMapping({
            "/owner/payments/summary",
            "/cashier/payments/summary"
    })
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PaymentSummaryResponse getPaymentSummary(
            @RequestParam(required = false) String student,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        Long centerId = TenantContext.getCurrentTenantId();
        return paymentService.getPaymentSummary(centerId, student, cashierId, method, status, startDate, endDate);
    }


    @GetMapping({
            "/owner/payments/{paymentId}/receipt",
            "/cashier/payments/{paymentId}/receipt"
    })
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PaymentResponse getReceipt(@PathVariable Long paymentId) {
        return paymentService.getReceipt(paymentId);
    }

    @PostMapping("/owner/payments/{paymentId}/void")
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public PaymentResponse voidPayment(@PathVariable Long paymentId,
                                        @RequestParam String reason) {
        return paymentService.voidPayment(paymentId, reason);
    }

    @PostMapping("/owner/payments/{paymentId}/refund")
    @PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
    public PaymentResponse refund(@PathVariable Long paymentId,
                                   @RequestParam BigDecimal amount,
                                   @RequestParam(required = false) String reason) {
        return paymentService.refund(paymentId, amount, reason);
    }

    @GetMapping({"/owner/students/{studentId}/timeline", "/cashier/students/{studentId}/timeline"})
    @PreAuthorize("hasAnyAuthority('PAYMENT_VIEW', 'FEE_VIEW')")
    public List<TimelineEntryResponse> getFinancialTimeline(@PathVariable Long studentId) {
        return paymentService.getFinancialTimeline(studentId);
    }

    // Legacy: kept for backward compatibility (used by old frontend if any)
    @GetMapping({
            "/owner/payments/all",
            "/cashier/payments/all"
    })
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public List<PaymentResponse> findAllByCenterLegacy() {
        return paymentService.findAllByCenter();
    }
}
