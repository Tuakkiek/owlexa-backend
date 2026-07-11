package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private PaymentService paymentService;

    private static final Long CURRENT_CENTER_ID = 1L;
    private static final Long OTHER_CENTER_ID = 99L;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                userRepository, membershipRepository, feeRecordRepository, paymentRepository
        );
        TenantContext.setCurrentTenantId(CURRENT_CENTER_ID);

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setPhoneNumber("0901234567");
        currentUser.setRole(Role.OWNER);
        lenient().when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(currentUser));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(10L, CURRENT_CENTER_ID))
                .thenReturn(true);
        lenient().when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("0901234567", null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private FeeRecord buildFeeRecord(Long feeRecordId, Long centerId, BigDecimal amount, BigDecimal paid) {
        Center center = new Center();
        center.setId(centerId);

        Class clazz = new Class();
        clazz.setId(50L);

        User student = new User();
        student.setId(20L);
        student.setPhoneNumber("student-1");

        FeeRecord fr = new FeeRecord();
        fr.setId(feeRecordId);
        fr.setCenter(center);
        fr.setClazz(clazz);
        fr.setStudentUser(student);
        fr.setAmount(amount);
        fr.setPaidAmount(paid);
        fr.setStatus(FeeStatus.PARTIAL);
        fr.setMonth("2026-07");
        fr.setDueDate(LocalDate.now());
        return fr;
    }

    private CashPaymentRequest buildRequest(BigDecimal amount) {
        CashPaymentRequest req = new CashPaymentRequest();
        req.setAmount(amount);
        req.setNote("test");
        return req;
    }

    @Test
    @DisplayName("collectCash: fee record thuộc center khác → TenancyViolationException")
    void collectCash_whenFeeRecordBelongsToOtherCenter_shouldThrowTenancyViolation() {
        FeeRecord feeRecord = buildFeeRecord(99L, OTHER_CENTER_ID, new BigDecimal("1000"), BigDecimal.ZERO);
        when(feeRecordRepository.findById(99L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(99L, buildRequest(new BigDecimal("100"))))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("collectCash: amount vượt remaining → BusinessRuleException")
    void collectCash_whenAmountExceedsRemaining_shouldThrowBusinessRule() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("800"));
        when(feeRecordRepository.findById(50L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(50L, buildRequest(new BigDecimal("500"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds remaining balance");
    }

    @Test
    @DisplayName("collectCash: amount = 0 → BadRequestException (validation)")
    void collectCash_whenAmountIsZero_shouldThrowBadRequest() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), BigDecimal.ZERO);
        when(feeRecordRepository.findById(50L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(50L, buildRequest(BigDecimal.ZERO)))
                .isInstanceOf(com.owlexa.owlexabackend.common.exception.BadRequestException.class);
    }

    @Test
    @DisplayName("collectCash: fee record không tồn tại → ResourceNotFoundException")
    void collectCash_whenFeeRecordNotFound_shouldThrowResourceNotFound() {
        when(feeRecordRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.collectCash(404L, buildRequest(new BigDecimal("100"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("collectCash: happy path → FeeRecord.status = PARTIAL khi chưa trả hết")
    void collectCash_whenValidAndPartial_shouldUpdateFeeStatusToPartial() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("500"));
        when(feeRecordRepository.findById(50L)).thenReturn(Optional.of(feeRecord));

        paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(feeRecord.getPaidAmount()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(feeRecord.getStatus()).isEqualTo(FeeStatus.PARTIAL);
    }

    @Test
    @DisplayName("collectCash: trả hết → FeeRecord.status = PAID")
    void collectCash_whenFullPayment_shouldUpdateFeeStatusToPaid() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("700"));
        when(feeRecordRepository.findById(50L)).thenReturn(Optional.of(feeRecord));

        paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(feeRecord.getStatus()).isEqualTo(FeeStatus.PAID);
    }
}