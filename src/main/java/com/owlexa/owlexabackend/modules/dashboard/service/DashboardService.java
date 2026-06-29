package com.owlexa.owlexabackend.modules.dashboard.service;
import com.owlexa.owlexabackend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.filter.TenantFilter;
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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MembershipRepository membershipRepository;
    private final ClassRepository classRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getOwnerStats() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can access dashboard stats");
        }

        long totalStudents = membershipRepository.countByCenterIdAndUserRole(centerId, Role.STUDENT);
        long totalTeachers = membershipRepository.countByCenterIdAndUserRole(centerId, Role.TEACHER);
        long totalClasses  = classRepository.countByCenterId(centerId);
        long totalFeeRecords  = feeRecordRepository.countByCenterId(centerId);
        long unpaidFeeRecords = feeRecordRepository.countByCenterIdAndStatus(centerId, FeeStatus.UNPAID);
        long paidFeeRecords   = feeRecordRepository.countByCenterIdAndStatus(centerId, FeeStatus.PAID);
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
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new AccessDeniedException("X-Tenant-ID header is required");
        }
        return centerId;
    }
}
