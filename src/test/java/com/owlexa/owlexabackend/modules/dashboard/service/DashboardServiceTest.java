package com.owlexa.owlexabackend.modules.dashboard.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.DiscountRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ClassRepository classRepository;
    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private InstallmentRepository installmentRepository;

    private DashboardService service;

    private static final String PHONE = "0901234567";
    private static final Long CENTER_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new DashboardService(
                membershipRepository, classRepository, feeRecordRepository,
                paymentRepository, userRepository,
                discountRepository, refundRepository, installmentRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PHONE, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Role role) {
        User user = new User();
        user.setId(10L);
        user.setPhoneNumber(PHONE);
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("Happy path: OWNER + tenant có data → trả về stats đầy đủ")
    void getOwnerStats_whenOwnerAndCenterHasData_shouldReturnCompleteStats() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(membershipRepository.countByCenter_IdAndUserRole(CENTER_ID, Role.STUDENT))
                .thenReturn(50L);
        when(membershipRepository.countByCenter_IdAndUserRole(CENTER_ID, Role.TEACHER))
                .thenReturn(5L);
        when(classRepository.countByCenter_Id(CENTER_ID)).thenReturn(8L);
        when(feeRecordRepository.countByCenter_Id(CENTER_ID)).thenReturn(120L);
        when(feeRecordRepository.countByCenter_IdAndStatusIn(CENTER_ID, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)))
                .thenReturn(20L);
        when(feeRecordRepository.countByCenter_IdAndStatus(CENTER_ID, FeeStatus.PAID))
                .thenReturn(90L);
        when(paymentRepository.sumAmountByCenterId(CENTER_ID))
                .thenReturn(new BigDecimal("150000000"));

        DashboardStatsResponse stats = service.getOwnerStats();

        assertThat(stats.getTotalStudents()).isEqualTo(50L);
        assertThat(stats.getTotalTeachers()).isEqualTo(5L);
        assertThat(stats.getTotalClasses()).isEqualTo(8L);
        assertThat(stats.getTotalFeeRecords()).isEqualTo(120L);
        assertThat(stats.getUnpaidFeeRecords()).isEqualTo(20L);
        assertThat(stats.getPaidFeeRecords()).isEqualTo(90L);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("150000000"));
    }

    @Test
    @DisplayName("Center rỗng (mới tạo) → stats đều 0, revenue = 0")
    void getOwnerStats_whenCenterIsEmpty_shouldReturnZeroStats() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        when(membershipRepository.countByCenter_IdAndUserRole(CENTER_ID, Role.STUDENT))
                .thenReturn(0L);
        when(membershipRepository.countByCenter_IdAndUserRole(CENTER_ID, Role.TEACHER))
                .thenReturn(0L);
        when(classRepository.countByCenter_Id(CENTER_ID)).thenReturn(0L);
        when(feeRecordRepository.countByCenter_Id(CENTER_ID)).thenReturn(0L);
        when(feeRecordRepository.countByCenter_IdAndStatusIn(CENTER_ID, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)))
                .thenReturn(0L);
        when(feeRecordRepository.countByCenter_IdAndStatus(CENTER_ID, FeeStatus.PAID))
                .thenReturn(0L);
        when(paymentRepository.sumAmountByCenterId(CENTER_ID)).thenReturn(BigDecimal.ZERO);

        DashboardStatsResponse stats = service.getOwnerStats();

        assertThat(stats.getTotalStudents()).isZero();
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("User không phải OWNER (vd STUDENT) → AccessDeniedException")
    void getOwnerStats_whenUserIsNotOwner_shouldThrowAccessDenied() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.STUDENT)));

        assertThatThrownBy(() -> service.getOwnerStats())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("User không phải OWNER (vd TEACHER) → AccessDeniedException")
    void getOwnerStats_whenUserIsTeacher_shouldThrowAccessDenied() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.TEACHER)));

        assertThatThrownBy(() -> service.getOwnerStats())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("TenantContext null (chưa qua JwtFilter) → AccessDeniedException")
    void getOwnerStats_whenTenantContextIsNull_shouldThrowAccessDenied() {
        when(userRepository.findByPhoneNumber(PHONE))
                .thenReturn(Optional.of(buildUser(Role.OWNER)));
        TenantContext.clear();

        assertThatThrownBy(() -> service.getOwnerStats())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Tenant context");
    }

    @Test
    @DisplayName("User không tồn tại trong DB → ResourceNotFoundException")
    void getOwnerStats_whenUserNotFoundInDb_shouldThrowResourceNotFound() {
        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnerStats())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Chưa authenticate (anonymousUser) → AccessDeniedException")
    void getOwnerStats_whenNotAuthenticated_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getOwnerStats())
                .isInstanceOf(AccessDeniedException.class);
    }
}