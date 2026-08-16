package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.SePayWebhookEventRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private BankTransferQrService bankTransferQrService;
    @Mock private SePayWebhookEventRepository sePayWebhookEventRepository;

    private PaymentService paymentService;

    private static final Long CURRENT_CENTER_ID = 1L;
    private static final Long OTHER_CENTER_ID = 99L;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                userRepository, membershipRepository, feeRecordRepository, paymentRepository,
                auditLogRepository, installmentRepository, refundRepository,
                classEnrollmentRepository, bankTransferQrService, sePayWebhookEventRepository
        );
        TenantContext.setCurrentTenantId(CURRENT_CENTER_ID);

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setPhoneNumber("0901234567");
        currentUser.setRole(Role.CASHIER);
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
        lenient().when(paymentRepository.findMaxReceiptNumberByPrefix(any(String.class)))
                .thenReturn(null);
        lenient().when(paymentRepository.findValidPendingByFeeRecordAndStudent(any(Long.class), any(Long.class), any(Instant.class)))
                .thenReturn(List.of());
        lenient().when(paymentRepository.findExpiredPendingByFeeRecordAndStudent(any(Long.class), any(Long.class), any(Instant.class)))
                .thenReturn(List.of());

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

    private Payment buildPendingPayment(Long id, FeeRecord feeRecord, BigDecimal amount) {
        User collector = new User();
        collector.setId(10L);
        collector.setPhoneNumber("0901234567");
        collector.setRole(Role.CASHIER);

        Payment payment = new Payment();
        payment.setId(id);
        payment.setReceiptNumber("RCP-20260817-000001");
        payment.setFeeRecord(feeRecord);
        payment.setCenter(feeRecord.getCenter());
        payment.setStudentUser(feeRecord.getStudentUser());
        payment.setCollectedByUser(collector);
        payment.setAmount(amount);
        payment.setMethod(PaymentMethod.SEPAY);
        payment.setStatus(TransactionStatus.PENDING);
        payment.setSepayRef("OWX" + String.format("%06d", id));
        payment.setExpiresAt(Instant.now().plusSeconds(600));
        return payment;
    }

    @Test
    @DisplayName("collectCash: fee record thuộc center khác → TenancyViolationException")
    void collectCash_whenFeeRecordBelongsToOtherCenter_shouldThrowTenancyViolation() {
        FeeRecord feeRecord = buildFeeRecord(99L, OTHER_CENTER_ID, new BigDecimal("1000"), BigDecimal.ZERO);
        when(paymentRepository.findFeeRecordByIdForUpdate(99L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(99L, buildRequest(new BigDecimal("100"))))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("collectCash: amount vượt remaining → BusinessRuleException")
    void collectCash_whenAmountExceedsRemaining_shouldThrowBusinessRule() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("800"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(50L, buildRequest(new BigDecimal("500"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Số tiền thanh toán vượt quá dư nợ còn lại");
    }

    @Test
    @DisplayName("collectCash: amount = 0 → BadRequestException (validation)")
    void collectCash_whenAmountIsZero_shouldThrowBadRequest() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), BigDecimal.ZERO);
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.collectCash(50L, buildRequest(BigDecimal.ZERO)))
                .isInstanceOf(com.owlexa.owlexabackend.common.exception.BadRequestException.class);
    }

    @Test
    @DisplayName("collectCash: fee record không tồn tại → ResourceNotFoundException")
    void collectCash_whenFeeRecordNotFound_shouldThrowResourceNotFound() {
        when(paymentRepository.findFeeRecordByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.collectCash(404L, buildRequest(new BigDecimal("100"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("collectCash: happy path → FeeRecord.status = PARTIAL khi chưa trả hết")
    void collectCash_whenValidAndPartial_shouldUpdateFeeStatusToPartial() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("500"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(feeRecord.getPaidAmount()).isEqualByComparingTo(new BigDecimal("800"));
        assertThat(feeRecord.getStatus()).isEqualTo(FeeStatus.PARTIAL);
    }

    @Test
    @DisplayName("collectCash: trả hết → FeeRecord.status = PAID")
    void collectCash_whenFullPayment_shouldUpdateFeeStatusToPaid() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("700"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(feeRecord.getStatus()).isEqualTo(FeeStatus.PAID);
    }

    @Test
    @DisplayName("collectCash: phải sinh receipt number định dạng RCP-YYYYMMDD-NNNNNN")
    void collectCash_shouldGenerateReceiptNumber() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("500"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        var response = paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(response.getReceiptNumber()).isNotNull();
        assertThat(response.getReceiptNumber()).matches("RCP-\\d{8}-\\d{6}");
    }

    @Test
    @DisplayName("collectCash: receipt number tăng tuần tự trong cùng ngày")
    void collectCash_shouldIncrementReceiptNumberSequentially() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("500"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        String todayPrefix = "RCP-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        when(paymentRepository.findMaxReceiptNumberByPrefix(todayPrefix)).thenReturn(todayPrefix + "000005");

        var response = paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(response.getReceiptNumber()).isEqualTo(todayPrefix + "000006");
    }

    @Test
    @DisplayName("collectCash: default payment method = CASH")
    void collectCash_shouldDefaultToCash() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("500"));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        var response = paymentService.collectCash(50L, buildRequest(new BigDecimal("300")));

        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    @DisplayName("collectCash: có QR pending hợp lệ → không cho cashier thu trực tiếp")
    void collectCash_whenValidPendingQrExists_shouldThrowBusinessRule() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), BigDecimal.ZERO);
        Payment pendingQr = buildPendingPayment(77L, feeRecord, new BigDecimal("1000"));

        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));
        when(paymentRepository.findValidPendingByFeeRecordAndStudent(any(Long.class), any(Long.class), any(Instant.class)))
                .thenReturn(List.of(pendingQr));

        assertThatThrownBy(() -> paymentService.collectCash(50L, buildRequest(new BigDecimal("1000"))))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(feeRecord.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(feeRecordRepository, never()).save(any(FeeRecord.class));
    }

    @Test
    @DisplayName("createPendingBankTransfer: web/cashier tái sử dụng QR pending thay vì tạo mã thứ hai")
    void createPendingBankTransfer_whenValidPendingExists_shouldReturnExistingPayment() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), BigDecimal.ZERO);
        Payment existing = buildPendingPayment(88L, feeRecord, new BigDecimal("1000"));

        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));
        when(paymentRepository.findValidPendingByFeeRecordAndStudent(any(Long.class), any(Long.class), any(Instant.class)))
                .thenReturn(List.of(existing));

        var response = paymentService.createPendingBankTransfer(50L, buildRequest(new BigDecimal("1000")));

        assertThat(response.getId()).isEqualTo(88L);
        assertThat(response.getSepayRef()).isEqualTo("OWX000088");
        verify(paymentRepository, never()).findExpiredPendingByFeeRecordAndStudent(any(), any(), any());
    }

    @Test
    @DisplayName("confirmBankTransferPayment: không cộng tiền lần hai khi fee record đã thanh toán đủ")
    void confirmBankTransferPayment_whenFeeAlreadyPaid_shouldVoidDuplicateWithoutIncreasingPaidAmount() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("1000"));
        feeRecord.setStatus(FeeStatus.PAID);
        Payment duplicate = buildPendingPayment(89L, feeRecord, new BigDecimal("1000"));

        when(paymentRepository.findByIdForUpdate(89L)).thenReturn(Optional.of(duplicate));
        when(paymentRepository.findFeeRecordByIdForUpdate(50L)).thenReturn(Optional.of(feeRecord));

        assertThatThrownBy(() -> paymentService.confirmBankTransferPayment(89L, new com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(PaymentService.DUPLICATE_PAYMENT_CODE);

        assertThat(duplicate.getStatus()).isEqualTo(TransactionStatus.VOIDED);
        assertThat(feeRecord.getPaidAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        verify(feeRecordRepository, never()).save(any(FeeRecord.class));
    }

    @Test
    @DisplayName("confirmBankTransferPayment: chuyển lại cùng QR đã ACTIVE → DUPLICATE_PAYMENT")
    void confirmBankTransferPayment_whenPaymentAlreadyActive_shouldThrowDuplicateCode() {
        FeeRecord feeRecord = buildFeeRecord(50L, CURRENT_CENTER_ID,
                new BigDecimal("1000"), new BigDecimal("1000"));
        Payment active = buildPendingPayment(90L, feeRecord, new BigDecimal("1000"));
        active.setStatus(TransactionStatus.ACTIVE);

        when(paymentRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> paymentService.confirmBankTransferPayment(90L, new com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(PaymentService.DUPLICATE_PAYMENT_CODE);
    }
}
