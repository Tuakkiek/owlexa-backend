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
 * Daily job that automatically drops students with unpaid overdue fees.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Find all UNPAID FeeRecords with dueDate before today</li>
 *   <li>For each, find the corresponding active ClassEnrollment</li>
 *   <li>Set enrollment status to DROPPED</li>
 * </ol>
 *
 * <p>Idempotent: safe to run multiple times.
 * Only drops ACTIVE enrollments — already DROPPED/PENDING are skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueEnrollmentJob {

    private final FeeRecordRepository feeRecordRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    @Value("${app.enrollment.overdue-cron:0 0 2 * * *}")
    @SuppressWarnings("unused")
    private String cronExpression;

    @Scheduled(cron = "${app.enrollment.overdue-cron:0 0 2 * * *}")
    @Transactional
    public void dropOverdueEnrollments() {
        LocalDate today = LocalDate.now();

        List<FeeRecord> overdueFees = feeRecordRepository
                .findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, today);

        if (overdueFees.isEmpty()) {
            return;
        }

        int droppedCount = 0;
        for (FeeRecord fee : overdueFees) {
            if (fee.getClazz() == null) continue;

            classEnrollmentRepository
                    .findByClazz_IdAndStudentUser_Id(
                            fee.getClazz().getId(),
                            fee.getStudentUser().getId())
                    .ifPresent(enrollment -> {
                        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                            enrollment.setStatus(EnrollmentStatus.DROPPED);
                            classEnrollmentRepository.save(enrollment);
                            log.info("OverdueEnrollmentJob: dropped enrollment {} — studentId={} classId={} feeId={}",
                                    enrollment.getId(),
                                    fee.getStudentUser().getId(),
                                    fee.getClazz().getId(),
                                    fee.getId());
                        }
                    });
            droppedCount++;
        }

        if (droppedCount > 0) {
            log.info("OverdueEnrollmentJob: processed {} overdue fee records, dropped ACTIVE enrollments",
                    droppedCount);
        }
    }
}
