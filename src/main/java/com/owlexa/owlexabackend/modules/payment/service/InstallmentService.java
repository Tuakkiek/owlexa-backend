package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.payment.dto.request.InstallmentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.InstallmentScheduleRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.InstallmentResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.Installment;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
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
public class InstallmentService {

    private final InstallmentRepository installmentRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<InstallmentResponse> createSchedule(Long feeRecordId, InstallmentScheduleRequest request) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));

        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(discount);

        BigDecimal totalExpected = request.getInstallments().stream()
                .map(i -> i.getExpectedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpected.compareTo(effectiveAmount) != 0) {
            throw new BusinessRuleException("Total installments (" + totalExpected
                    + ") must equal tuition after discount (" + effectiveAmount + ")");
        }

        // Delete existing installments
        List<Installment> existing = installmentRepository.findAllByFeeRecord_IdOrderByDueDateAsc(feeRecordId);
        installmentRepository.deleteAll(existing);

        List<Installment> installments = request.getInstallments().stream().map(i ->
                Installment.builder()
                        .feeRecord(feeRecord).center(feeRecord.getCenter())
                        .dueDate(i.getDueDate()).expectedAmount(i.getExpectedAmount())
                        .paidAmount(BigDecimal.ZERO).status(InstallmentStatus.PENDING)
                        .build()
        ).toList();
        List<Installment> saved = installmentRepository.saveAll(installments);

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> findByFeeRecord(Long feeRecordId) {
        User currentUser = getCurrentUser();
        assertCanView(currentUser);
        return installmentRepository.findAllByFeeRecord_IdOrderByDueDateAsc(feeRecordId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public InstallmentResponse updateInstallment(Long installmentId, InstallmentRequest request) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

        if (installment.getStatus() == InstallmentStatus.PAID || installment.getStatus() == InstallmentStatus.PARTIALLY_PAID) {
            throw new BusinessRuleException("Cannot modify an already paid or partially paid installment");
        }

        installment.setDueDate(request.getDueDate());
        installment.setExpectedAmount(request.getExpectedAmount());
        installment = installmentRepository.save(installment);

        // Validate total still matches
        FeeRecord feeRecord = installment.getFeeRecord();
        validateTotalMatches(feeRecord);

        return toResponse(installment);
    }

    @Transactional
    public void deleteInstallment(Long installmentId) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

        if (installment.getStatus() != InstallmentStatus.PENDING) {
            throw new BusinessRuleException("Cannot delete a paid or partially paid installment");
        }

        FeeRecord feeRecord = installment.getFeeRecord();
        installmentRepository.delete(installment);

        // Validate remaining total still matches
        validateTotalMatches(feeRecord);
    }

    private void validateTotalMatches(FeeRecord feeRecord) {
        BigDecimal discount = feeRecord.getDiscountAmount() != null ? feeRecord.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(discount);
        List<Installment> current = installmentRepository.findAllByFeeRecord_IdOrderByDueDateAsc(feeRecord.getId());
        BigDecimal totalExpected = current.stream()
                .map(Installment::getExpectedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalExpected.compareTo(effectiveAmount) != 0) {
            throw new BusinessRuleException("Installment total (" + totalExpected
                    + ") must equal tuition after discount (" + effectiveAmount + ")");
        }
    }

    private InstallmentResponse toResponse(Installment i) {
        BigDecimal remaining = i.getExpectedAmount().subtract(i.getPaidAmount());
        return InstallmentResponse.builder()
                .id(i.getId()).feeRecordId(i.getFeeRecord().getId())
                .dueDate(i.getDueDate()).expectedAmount(i.getExpectedAmount())
                .paidAmount(i.getPaidAmount())
                .remainingAmount(remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining)
                .status(i.getStatus()).build();
    }

    private void assertOwner(User user) {
        if (user.getRole() != Role.OWNER) throw new AccessDeniedException("Only OWNER can manage installments");
    }

    private void assertCanView(User user) {
        if (user.getRole() != Role.OWNER && user.getRole() != Role.CASHIER)
            throw new AccessDeniedException("Not authorized");
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Not authenticated");
        return userRepository.findByPhoneNumber(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
