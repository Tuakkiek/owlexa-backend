package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.dto.request.RefundDecisionRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.RefundPayoutRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.RefundRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.RefundResponse;
import com.owlexa.owlexabackend.modules.payment.entity.*;
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    // ── Create refund request ─────────────────────────────────────────────

    @Transactional
    public RefundResponse requestRefund(RefundRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giao dịch thanh toán với ID: " + request.getPaymentId()));

        if (!payment.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Giao dịch thanh toán không thuộc trung tâm hiện tại.");
        }

        if (payment.getStatus() != TransactionStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ có thể hoàn tiền cho giao dịch đang hoạt động.");
        }

        // Check total non-rejected refunds (REQUESTED + APPROVED + PAID) don't exceed payment
        BigDecimal alreadyCommitted = refundRepository.sumNonRejectedAmountByPaymentId(payment.getId());
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyCommitted);

        if (request.getAmount().compareTo(maxRefundable) > 0) {
            throw new BusinessRuleException(
                    "Số tiền hoàn trả vượt quá số dư khả dụng. Tối đa có thể hoàn: " + maxRefundable + " VND");
        }

        ClassEnrollment relatedEnrollment = null;
        if (request.getRelatedEnrollmentId() != null) {
            relatedEnrollment = classEnrollmentRepository.findById(request.getRelatedEnrollmentId())
                    .orElse(null);
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .center(payment.getCenter())
                .amount(request.getAmount())
                .reason(request.getReason())
                .createdBy(currentUser)
                .requestedBy(currentUser)
                .status(RefundStatus.REQUESTED)
                .relatedEnrollment(relatedEnrollment)
                .build();

        refund = refundRepository.save(refund);

        writeAuditLog(currentUser, payment.getCenter(), "REFUND_REQUESTED", "Refund",
                refund.getId(), "Yêu cầu hoàn " + request.getAmount() + " VND từ biên lai "
                        + payment.getReceiptNumber() + " - Lý do: " + request.getReason());

        return toResponse(refund);
    }

    // ── Approve / Reject ──────────────────────────────────────────────────

    @Transactional
    public RefundResponse decide(Long refundId, RefundDecisionRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền ID: " + refundId));

        if (!refund.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Yêu cầu hoàn tiền không thuộc trung tâm hiện tại.");
        }

        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessRuleException(
                    "Chỉ có thể duyệt/từ chối yêu cầu đang ở trạng thái REQUESTED. Trạng thái hiện tại: "
                            + refund.getStatus());
        }

        if (Boolean.TRUE.equals(request.getApprove())) {
            refund.setStatus(RefundStatus.APPROVED);
            refund.setApprovedBy(currentUser);
            refund.setApprovedAt(Instant.now());

            writeAuditLog(currentUser, refund.getCenter(), "REFUND_APPROVED", "Refund",
                    refund.getId(), "Duyệt hoàn tiền " + refund.getAmount() + " VND");
        } else {
            if (request.getRejectedReason() == null || request.getRejectedReason().isBlank()) {
                throw new BusinessRuleException("Phải nhập lý do khi từ chối yêu cầu hoàn tiền.");
            }
            refund.setStatus(RefundStatus.REJECTED);
            refund.setRejectedReason(request.getRejectedReason());
            refund.setApprovedBy(currentUser);
            refund.setApprovedAt(Instant.now());

            writeAuditLog(currentUser, refund.getCenter(), "REFUND_REJECTED", "Refund",
                    refund.getId(), "Từ chối hoàn tiền - Lý do: " + request.getRejectedReason());
        }

        refund = refundRepository.save(refund);
        return toResponse(refund);
    }

    // ── Mark as paid ──────────────────────────────────────────────────────

    @Transactional
    public RefundResponse markPaid(Long refundId, RefundPayoutRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        // Validate refund method — only CASH or BANK_TRANSFER for refund payouts
        if (request.getRefundMethod() != PaymentMethod.CASH
                && request.getRefundMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessRuleException("Phương thức hoàn tiền chỉ hỗ trợ CASH hoặc BANK_TRANSFER.");
        }

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền ID: " + refundId));

        if (!refund.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Yêu cầu hoàn tiền không thuộc trung tâm hiện tại.");
        }

        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Chỉ có thể chi tiền cho yêu cầu đã được duyệt. Trạng thái hiện tại: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.PAID);
        refund.setRefundMethod(request.getRefundMethod());
        refund = refundRepository.save(refund);

        // Decrease paid amount on FeeRecord
        FeeRecord feeRecord = refund.getPayment().getFeeRecord();
        feeRecord.setPaidAmount(feeRecord.getPaidAmount().subtract(refund.getAmount()));
        feeRecord.setStatus(resolveFeeStatus(feeRecord.getAmount(), feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        writeAuditLog(currentUser, refund.getCenter(), "REFUND_PAID", "Refund",
                refund.getId(), "Chi hoàn tiền " + refund.getAmount() + " VND qua "
                        + request.getRefundMethod() + " - Biên lai: "
                        + refund.getPayment().getReceiptNumber());

        return toResponse(refund);
    }

    // ── List refunds ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RefundResponse> findAll(RefundStatus status) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        List<Refund> refunds;
        if (status != null) {
            refunds = refundRepository.findAllByCenter_IdAndStatusOrderByCreatedAtDesc(centerId, status);
        } else {
            refunds = refundRepository.findAllByCenter_IdOrderByCreatedAtDesc(centerId);
        }

        return refunds.stream().map(this::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private RefundResponse toResponse(Refund refund) {
        Payment payment = refund.getPayment();
        User student = payment.getStudentUser();

        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(payment.getId())
                .centerId(refund.getCenter().getId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .refundMethod(refund.getRefundMethod())
                .createdByUserId(refund.getCreatedBy().getId())
                .createdByUserName(refund.getCreatedBy().getFullName())
                .createdAt(refund.getCreatedAt())
                .requestedByUserId(refund.getRequestedBy() != null ? refund.getRequestedBy().getId() : null)
                .requestedByUserName(refund.getRequestedBy() != null ? refund.getRequestedBy().getFullName() : null)
                .approvedByUserId(refund.getApprovedBy() != null ? refund.getApprovedBy().getId() : null)
                .approvedByUserName(refund.getApprovedBy() != null ? refund.getApprovedBy().getFullName() : null)
                .approvedAt(refund.getApprovedAt())
                .rejectedReason(refund.getRejectedReason())
                .relatedEnrollmentId(refund.getRelatedEnrollment() != null ? refund.getRelatedEnrollment().getId() : null)
                .paymentReceiptNumber(payment.getReceiptNumber())
                .paymentAmount(payment.getAmount())
                .studentFullName(student.getFullName())
                .studentPhoneNumber(student.getPhoneNumber())
                .build();
    }

    private FeeStatus resolveFeeStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return FeeStatus.UNPAID;
        }
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return FeeStatus.PAID;
        }
        return FeeStatus.PARTIAL;
    }

    private void writeAuditLog(User user, Center center, String action, String entityType, Long entityId, String description) {
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

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User not authenticated");
        }
        String phoneNumber = authentication.getName();
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại."));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Không xác định được trung tâm hiện tại.");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm hiện tại.");
        }
    }
}
