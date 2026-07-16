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
    @DisplayName("dropOverdueEnrollments: UNPAID + past dueDate → drops ACTIVE enrollment")
    void whenUnpaidAndPastDue_shouldDropEnrollment() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, LocalDate.now()))
                .thenReturn(List.of(unpaid));

        ClassEnrollment active = buildEnrollment(EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(active));

        job.dropOverdueEnrollments();

        verify(classEnrollmentRepository).save(active);
        assert active.getStatus() == EnrollmentStatus.DROPPED;
    }

    @Test
    @DisplayName("dropOverdueEnrollments: UNPAID but dueDate is future → no action")
    void whenUnpaidButNotDue_shouldNotDrop() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().plusDays(5));
        when(feeRecordRepository.findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, LocalDate.now()))
                .thenReturn(List.of()); // query only returns past-due

        job.dropOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("dropOverdueEnrollments: PAID + past dueDate → no action")
    void whenPaidAndPastDue_shouldNotDrop() {
        // PAID records are not returned by the query (only UNPAID)
        when(feeRecordRepository.findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, LocalDate.now()))
                .thenReturn(List.of());

        job.dropOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("dropOverdueEnrollments: enrollment already DROPPED → no change")
    void whenAlreadyDropped_shouldNotModify() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, LocalDate.now()))
                .thenReturn(List.of(unpaid));

        ClassEnrollment dropped = buildEnrollment(EnrollmentStatus.DROPPED);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(dropped));

        job.dropOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("dropOverdueEnrollments: enrollment is PENDING → no change")
    void whenPending_shouldNotModify() {
        FeeRecord unpaid = buildFeeRecord(FEE_ID, FeeStatus.UNPAID, LocalDate.now().minusDays(1));
        when(feeRecordRepository.findAllByStatusAndDueDateBefore(FeeStatus.UNPAID, LocalDate.now()))
                .thenReturn(List.of(unpaid));

        ClassEnrollment pending = buildEnrollment(EnrollmentStatus.PENDING);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(CLASS_ID, STUDENT_ID))
                .thenReturn(Optional.of(pending));

        job.dropOverdueEnrollments();

        verify(classEnrollmentRepository, never()).save(any());
    }
}
