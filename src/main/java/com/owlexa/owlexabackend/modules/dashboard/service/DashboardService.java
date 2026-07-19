package com.owlexa.owlexabackend.modules.dashboard.service;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.CashierDashboardStatsResponse;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.RevenueSummaryResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserPermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.PermissionRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.DiscountRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestQuestionRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptRepository;
import com.owlexa.owlexabackend.modules.mocktest.repository.MockTestAttemptAnswerRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssaySubmissionRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayRubricRepository;
import com.owlexa.owlexabackend.modules.essay.repository.EssayGradingResultRepository;
import com.owlexa.owlexabackend.modules.document.repository.StudentDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MembershipRepository membershipRepository;
    private final ClassRepository classRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;
    private final RefundRepository refundRepository;
    private final InstallmentRepository installmentRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getOwnerStats() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can access dashboard stats");
        }

        long totalStudents = membershipRepository.countByCenter_IdAndUserRole(centerId, Role.STUDENT);
        long totalTeachers = membershipRepository.countByCenter_IdAndUserRole(centerId, Role.TEACHER);
        long totalClasses  = classRepository.countByCenter_Id(centerId);
        long totalFeeRecords  = feeRecordRepository.countByCenter_Id(centerId);
        long unpaidFeeRecords = feeRecordRepository.countByCenter_IdAndStatusIn(centerId, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL));
        long paidFeeRecords   = feeRecordRepository.countByCenter_IdAndStatus(centerId, FeeStatus.PAID);
        var  totalRevenue     = paymentRepository.sumAmountByCenterId(centerId);

        return DashboardStatsResponse.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .totalFeeRecords(totalFeeRecords)
                .unpaidFeeRecords(unpaidFeeRecords)
                .paidFeeRecords(paidFeeRecords)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public CashierDashboardStatsResponse getCashierStats() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only CASHIER can access cashier dashboard stats");
        }

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfNextDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long totalPaymentsToday = paymentRepository.countByCenterIdAndCreatedAtBetween(centerId, startOfDay, startOfNextDay);
        var totalAmountCollectedToday = paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, startOfDay, startOfNextDay);
        long totalPendingPayments = feeRecordRepository.countByCenter_IdAndStatusIn(centerId, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL));
        var totalPendingAmount = feeRecordRepository.sumRemainingByCenterIdAndStatusIn(centerId, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL));

        // This month stats
        Instant startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfNextMonth = today.with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long totalPaymentsThisMonth = paymentRepository.countByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextMonth);

        return CashierDashboardStatsResponse.builder()
                .totalPaymentsToday(totalPaymentsToday)
                .totalAmountCollectedToday(totalAmountCollectedToday)
                .totalPendingPayments(totalPendingPayments)
                .totalPendingAmount(totalPendingAmount)
                .totalPaymentsThisMonth(totalPaymentsThisMonth)
                .build();
    }

    // ── Revenue summaries ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BigDecimal getThisWeekRevenue() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can view revenue");
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        Instant weekStart = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, weekStart, weekEnd);
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummary() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.CASHIER) {
            throw new AccessDeniedException("Only OWNER or CASHIER can view revenue");
        }

        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfNextDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfYesterday = today.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant();

        BigDecimal todayRevenue = paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, startOfDay, startOfNextDay);
        BigDecimal yesterdayRevenue = paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, startOfYesterday, startOfDay);
        BigDecimal thisWeekRevenue = paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, startOfWeek, startOfNextDay);
        BigDecimal thisMonthRevenue = paymentRepository.sumAmountByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextDay);

        long todayCount = paymentRepository.countByCenterIdAndCreatedAtBetween(centerId, startOfDay, startOfNextDay);
        long thisMonthCount = paymentRepository.countByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextDay);

        BigDecimal avgAmount = paymentRepository.avgAmountByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextDay);
        BigDecimal maxAmount = paymentRepository.maxAmountByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextDay);
        BigDecimal minAmount = paymentRepository.minAmountByCenterIdAndCreatedAtBetween(centerId, startOfMonth, startOfNextDay);

        // Payment method breakdown (this month)
        Map<String, BigDecimal> methodBreakdown = new LinkedHashMap<>();
        Map<String, Long> methodCounts = new LinkedHashMap<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            BigDecimal sum = paymentRepository.sumAmountByCenterIdAndMethodAndCreatedAtBetween(centerId, pm, startOfMonth, startOfNextDay);
            long count = paymentRepository.countByCenterIdAndMethodAndCreatedAtBetween(centerId, pm, startOfMonth, startOfNextDay);
            if (sum.compareTo(BigDecimal.ZERO) > 0 || count > 0) {
                methodBreakdown.put(pm.name(), sum);
                methodCounts.put(pm.name(), count);
            }
        }

        // Extended stats
        BigDecimal grossRevenue = paymentRepository.sumAmountByCenterId(centerId);
        BigDecimal refundTotal = refundRepository.sumAmountByCenterId(centerId);
        BigDecimal discountTotal = BigDecimal.ZERO; // Discount tracked on FeeRecord — sum via query if needed
        BigDecimal netRevenue = grossRevenue.subtract(refundTotal);

        BigDecimal outstandingTuition = feeRecordRepository.sumRemainingByCenterIdAndStatusIn(centerId,
                List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL));
        BigDecimal overdueTuition = feeRecordRepository.sumRemainingByCenterIdAndStatusIn(centerId,
                List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)); // Same query — overdue is derived from status + due date

        return RevenueSummaryResponse.builder()
                .todayRevenue(todayRevenue)
                .yesterdayRevenue(yesterdayRevenue)
                .thisWeekRevenue(thisWeekRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .grossRevenue(grossRevenue)
                .discountTotal(discountTotal)
                .refundTotal(refundTotal)
                .netRevenue(netRevenue)
                .outstandingTuition(outstandingTuition)
                .overdueTuition(overdueTuition)
                .todayTransactionCount(todayCount)
                .thisMonthTransactionCount(thisMonthCount)
                .averagePaymentAmount(avgAmount)
                .highestPaymentAmount(maxAmount)
                .lowestPaymentAmount(minAmount)
                .methodBreakdown(methodBreakdown)
                .methodCounts(methodCounts)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phoneNumber: " + phoneNumber));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new AccessDeniedException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }
}
