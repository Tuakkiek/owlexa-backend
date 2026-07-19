package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.payment.dto.request.DiscountRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.DiscountResponse;
import com.owlexa.owlexabackend.modules.payment.entity.Discount;
import com.owlexa.owlexabackend.modules.payment.entity.DiscountType;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.DiscountRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public DiscountResponse create(Long feeRecordId, DiscountRequest request) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        FeeRecord feeRecord = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));

        if (feeRecord.getStatus() == FeeStatus.PAID) {
            throw new BusinessRuleException("Cannot apply discount to a fully paid tuition");
        }

        validateDiscount(request, feeRecord);

        BigDecimal discountAmount = computeDiscountAmount(request.getType(), request.getValue(), feeRecord.getAmount());

        Discount discount = Discount.builder()
                .feeRecord(feeRecord)
                .center(feeRecord.getCenter())
                .name(request.getName())
                .type(request.getType())
                .value(request.getValue())
                .reason(normalizeText(request.getReason()))
                .createdBy(currentUser)
                .build();
        discount = discountRepository.save(discount);

        // Update FeeRecord discountAmount
        BigDecimal totalDiscount = feeRecord.getDiscountAmount() != null
                ? feeRecord.getDiscountAmount().add(discountAmount) : discountAmount;
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(totalDiscount);
        if (effectiveAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Total discount cannot exceed tuition amount");
        }
        feeRecord.setDiscountAmount(totalDiscount);
        feeRecord.setStatus(resolveStatus(effectiveAmount, feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        return toResponse(discount);
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> findByFeeRecord(Long feeRecordId) {
        User currentUser = getCurrentUser();
        assertCanView(currentUser);
        return discountRepository.findAllByFeeRecord_Id(feeRecordId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public DiscountResponse update(Long discountId, DiscountRequest request) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        FeeRecord feeRecord = discount.getFeeRecord();
        if (feeRecord.getStatus() == FeeStatus.PAID) {
            throw new BusinessRuleException("Cannot modify discount on a fully paid tuition");
        }

        validateDiscount(request, feeRecord);

        // Remove old discount amount, apply new
        BigDecimal oldAmount = computeDiscountAmount(discount.getType(), discount.getValue(), feeRecord.getAmount());
        BigDecimal newAmount = computeDiscountAmount(request.getType(), request.getValue(), feeRecord.getAmount());
        BigDecimal updatedTotal = feeRecord.getDiscountAmount().subtract(oldAmount).add(newAmount);
        if (updatedTotal.compareTo(BigDecimal.ZERO) < 0) updatedTotal = BigDecimal.ZERO;

        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(updatedTotal);
        if (effectiveAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Total discount cannot exceed tuition amount");
        }

        discount.setName(request.getName());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setReason(normalizeText(request.getReason()));
        discountRepository.save(discount);

        feeRecord.setDiscountAmount(updatedTotal);
        feeRecord.setStatus(resolveStatus(effectiveAmount, feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        return toResponse(discount);
    }

    @Transactional
    public void delete(Long discountId) {
        User currentUser = getCurrentUser();
        assertOwner(currentUser);

        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        FeeRecord feeRecord = discount.getFeeRecord();
        if (feeRecord.getStatus() == FeeStatus.PAID) {
            throw new BusinessRuleException("Cannot remove discount from a fully paid tuition");
        }

        BigDecimal discountAmount = computeDiscountAmount(discount.getType(), discount.getValue(), feeRecord.getAmount());
        BigDecimal newTotal = feeRecord.getDiscountAmount().subtract(discountAmount);
        feeRecord.setDiscountAmount(newTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newTotal);
        BigDecimal effectiveAmount = feeRecord.getAmount().subtract(feeRecord.getDiscountAmount());
        feeRecord.setStatus(resolveStatus(effectiveAmount, feeRecord.getPaidAmount()));
        feeRecordRepository.save(feeRecord);

        discountRepository.delete(discount);
    }

    private void validateDiscount(DiscountRequest request, FeeRecord feeRecord) {
        if (request.getType() == DiscountType.PERCENTAGE) {
            if (request.getValue().compareTo(new BigDecimal("100")) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100%");
            }
        }
        if (request.getType() == DiscountType.FIXED) {
            if (request.getValue().compareTo(feeRecord.getAmount()) > 0) {
                throw new BadRequestException("Fixed discount cannot exceed tuition amount");
            }
        }
    }

    private BigDecimal computeDiscountAmount(DiscountType type, BigDecimal value, BigDecimal tuitionAmount) {
        if (type == DiscountType.PERCENTAGE) {
            return tuitionAmount.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return value;
    }

    private FeeStatus resolveStatus(BigDecimal effectiveAmount, BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) return FeeStatus.UNPAID;
        if (paidAmount.compareTo(effectiveAmount) >= 0) return FeeStatus.PAID;
        return FeeStatus.PARTIAL;
    }

    private DiscountResponse toResponse(Discount d) {
        return DiscountResponse.builder()
                .id(d.getId()).feeRecordId(d.getFeeRecord().getId())
                .name(d.getName()).type(d.getType()).value(d.getValue())
                .reason(d.getReason())
                .createdByUserId(d.getCreatedBy().getId())
                .createdByUserName(d.getCreatedBy().getFullName())
                .createdAt(d.getCreatedAt()).build();
    }

    private void assertOwner(User user) {
        if (user.getRole() != Role.OWNER) throw new AccessDeniedException("Only OWNER can manage discounts");
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

    private String normalizeText(String text) {
        if (text == null) return null;
        String t = text.trim();
        return t.isEmpty() ? null : t;
    }
}
