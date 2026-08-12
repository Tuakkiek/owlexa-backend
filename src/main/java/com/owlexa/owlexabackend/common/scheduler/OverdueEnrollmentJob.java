package com.owlexa.owlexabackend.common.scheduler;

import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily job that suspends students with unpaid or partially-paid overdue fees.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Find all UNPAID or PARTIAL FeeRecords with dueDate before today</li>
 *   <li>For each, find the corresponding ACTIVE ClassEnrollment</li>
 *   <li>Set enrollment status to SUSPENDED (preserving all history)</li>
 * </ol>
 *
 * <p>SUSPENDED enrollments are automatically reactivated when the student
 * makes a payment that clears the overdue condition (see {@code PaymentService.collectCash}).
 *
 * <p>Idempotent: safe to run multiple times.
 * Only suspends ACTIVE enrollments — already SUSPENDED/DROPPED/PENDING are skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueEnrollmentJob {

    private final FeeRecordRepository feeRecordRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    /** Statuses that indicate the student still owes money and may become overdue. */
    private static final List<FeeStatus> NOT_FULLY_PAID = List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL);

    @Value("${app.enrollment.overdue-cron:0 0 2 * * *}")
    @SuppressWarnings("unused")
    private String cronExpression;

    @Value("${app.enrollment.auto-suspend-overdue-enabled:false}")
    private boolean autoSuspendOverdueEnabled;

    @Scheduled(cron = "${app.enrollment.overdue-cron:0 0 2 * * *}")
    @Transactional
    public void suspendOverdueEnrollments() {
        if (!autoSuspendOverdueEnabled) {
            log.debug("OverdueEnrollmentJob: automatic overdue suspension is disabled");
            return;
        }

        LocalDate today = LocalDate.now();

        List<FeeRecord> overdueFees = feeRecordRepository
                .findAllByStatusInAndDueDateBefore(NOT_FULLY_PAID, today);

        if (overdueFees.isEmpty()) {
            return;
        }

        int suspendedCount = 0;
        for (FeeRecord fee : overdueFees) {
            if (fee.getClazz() == null) continue;

            classEnrollmentRepository
                    .findByClazz_IdAndStudentUser_Id(
                            fee.getClazz().getId(),
                            fee.getStudentUser().getId())
                    .ifPresent(enrollment -> {
                        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                            enrollment.setStatus(EnrollmentStatus.SUSPENDED);
                            classEnrollmentRepository.save(enrollment);
                            log.info("OverdueEnrollmentJob: suspended enrollment {} — studentId={} classId={} feeId={}",
                                    enrollment.getId(),
                                    fee.getStudentUser().getId(),
                                    fee.getClazz().getId(),
                                    fee.getId());
                        }
                    });
            suspendedCount++;
        }

        if (suspendedCount > 0) {
            log.info("OverdueEnrollmentJob: processed {} overdue fee records, suspended ACTIVE enrollments",
                    suspendedCount);
        }
    }
}
