package com.owlexa.owlexabackend.common.scheduler;

import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueEnrollmentJobTest {

    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    private OverdueEnrollmentJob job;

    private static final Long CLASS_ID = 50L;
    private static final Long STUDENT_ID = 100L;
    private static final Long FEE_ID = 10L;

    @BeforeEach
    void setUp() {
        job = new OverdueEnrollmentJob(feeRecordRepository, classEnrollmentRepository);
    }

    private FeeRecord buildFeeRecord(Long feeId, FeeStatus status, LocalDate dueDate) {
        Class clazz = new Class();
        clazz.setId(CLASS_ID);

        User student = new User();
        student.setId(STUDENT_ID);

        FeeRecord fee = new FeeRecord();
        fee.setId(feeId);
        fee.setClazz(clazz);
        fee.setStudentUser(student);
        fee.setStatus(status);
        fee.setDueDate(dueDate);
        return fee;
    }

    private ClassEnrollment buildEnrollment(EnrollmentStatus status) {
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setId(1L);
        enrollment.setStatus(status);
        return enrollment;
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: UNPAID + past dueDate → suspends ACTIVE enrollment")
    void whenUnpaidAndPastDue_shouldSuspendEnrollment() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of(unpaid));

        ClassEnrollment active = buildEnrollment(EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(active));

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository).save(active);
        assert active.getStatus() == EnrollmentStatus.SUSPENDED;
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: PARTIAL + past dueDate → suspends ACTIVE enrollment")
    void whenPartialAndPastDue_shouldSuspendEnrollment() {
        FeeRecord partial = buildFeeRecord(FEE_ID, FeeStatus.PARTIAL, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of(partial));

        ClassEnrollment active = buildEnrollment(EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(active));

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository).save(active);
        assert active.getStatus() == EnrollmentStatus.SUSPENDED;
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: UNPAID but dueDate is future → no action")
    void whenUnpaidButNotDue_shouldNotSuspend() {
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of()); // query only returns past-due

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: PAID + past dueDate → no action")
    void whenPaidAndPastDue_shouldNotSuspend() {
        // PAID records are not returned by the query (only UNPAID + PARTIAL)
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of());

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: enrollment already SUSPENDED → no change")
    void whenAlreadySuspended_shouldNotModify() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of(unpaid));

        ClassEnrollment suspended = buildEnrollment(EnrollmentStatus.SUSPENDED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(suspended));

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: enrollment is PENDING → no change")
    void whenPending_shouldNotModify() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of(unpaid));

        ClassEnrollment pending = buildEnrollment(EnrollmentStatus.PENDING);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(pending));

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("suspendOverdueEnrollments: enrollment is DROPPED → no change")
    void whenDropped_shouldNotModify() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusInAndDueDateBefore(
                eq(List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL)), eq(LocalDate.now())))
                .thenReturn(List.of(unpaid));

        ClassEnrollment dropped = buildEnrollment(EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(dropped));

        job.suspendOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }
}
