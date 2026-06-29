package com.owlexa.owlexabackend.modules.payment.service;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentResponse;
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
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.filter.TenantFilter;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;


    // Collect cash
    @Transactional
    public PaymentResponse collectCash(Long feeRecordId, CashPaymentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found with id: " + feeRecordId));

        if(!feeRecord.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to manage this fee record");
        }

        validateAmount(request.getAmount());

        BigDecimal remainingAmount = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        if(request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BadRequestException("Payment amount exceeds remaining balance");
        }

        Payment payment = Payment.builder()
                .feeRecord(feeRecord)
                .center(feeRecord.getCenter())
                .studentUser(feeRecord.getStudentUser())
                .collectdByUser(currentUser)
                .amount(request.getAmount())
                .method(PaymentMethod.CASH)
                .note(normalizeText(request.getNote()))
                .build();

        payment = paymentRepository.save(payment);

        BigDecimal newPaidAmount = feeRecord.getPaidAmount().add(request.getAmount());
        feeRecord.setPaidAmount(newPaidAmount);
        feeRecord.setStatus(resloveFeeStatus(feeRecord.getAmount(), newPaidAmount));

        feeRecordRepository.save(feeRecord);

        return toResponse(payment, feeRecord);
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
            throw new BadRequestException("You do not have permission to manage this feeRecord");
        }

        return paymentRepository.findAllByFeeRecordOrderByCreatedAtDesc(feeRecordId)
                .stream()
                .map(payment -> toResponse(payment, feeRecord))
                .toList();
    }

    // Find my payments
    @Transactional(readOnly = true)
    public List<PaymentResponse> findMyPayments() {
        User currentUser = getCurrentUser();

        return paymentRepository.findAllByStudentUserIdOrderByCreatedAtDesc(currentUser.getId())
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

        return paymentRepository.findAllByCenterIdOrderByCreatedAtDesc(centerId)
                .stream()
                .map(payment -> toResponse(payment, payment.getFeeRecord()))
                .toList();
    }

    // Helper
    // To response
    private PaymentResponse toResponse(Payment payment, FeeRecord feeRecord) {

        BigDecimal remaining = feeRecord.getAmount().subtract(feeRecord.getPaidAmount());

        return PaymentResponse.builder()
                .id(payment.getId())
                .feeRecordId(feeRecord.getId())
                .centerId(feeRecord.getCenter().getId())
                .classId(feeRecord.getClazz().getId())
                .studentUserId(feeRecord.getStudentUser().getId())
                .studentPhoneNumber(feeRecord.getStudentUser().getPhoneNumber())
                .studentFullName(feeRecord.getStudentUser().getFullName())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .sepayRef(payment.getSepayRef())
                .note(payment.getNote())
                .collectedByUserId(payment.getCollectdByUser() != null ? payment.getCollectdByUser().getId() : null)
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
        Long centerId = TenantFilter.getCurrentCenterId();

        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    // Assert can collect payment
    private void assertCanCollectPayment(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can collect payment");
        }

        assertCenterMembership(currentUser, centerId);
    }

    // Assert center membership
    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);

        if (!hasMembership) {
            throw new AccessDeniedException("User is not member of this center");
        }
    }
}
