package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeRecordServiceTest {

    @Mock private FeeRecordRepository feeRecordRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private FeeRecordService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new FeeRecordService(
                feeRecordRepository, classEnrollmentRepository,
                userRepository, membershipRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);
        lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("findMyFees: trả về fee của student hiện tại")
    void findMyFees_shouldReturnCurrentStudentFees() {
        User student = new User();
        student.setId(100L);
        student.setPhoneNumber(OWNER_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(student));
        when(feeRecordRepository.findAllActiveEnrollmentFeesByStudentUserId(100L))
                .thenReturn(new ArrayList<>());

        List<FeeRecordResponse> responses = service.findMyFees();

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("findAllOverdue: query với status IN (UNPAID, PARTIAL) và dueDate < today")
    void findAllOverdue_shouldQueryWithUnpaidAndPastDueDate() {
        when(feeRecordRepository.findAllByCenter_IdAndStatusInAndDueDateBefore(
                org.mockito.ArgumentMatchers.eq(CENTER_ID),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(new ArrayList<>());

        List<FeeRecordResponse> responses = service.findAllOverdue();

        assertThat(responses).isEmpty();
        org.mockito.Mockito.verify(feeRecordRepository)
                .findAllByCenter_IdAndStatusInAndDueDateBefore(
                        org.mockito.ArgumentMatchers.eq(CENTER_ID),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.any(LocalDate.class)
                );
    }

    @Test
    @DisplayName("findAllPending: tự khôi phục fee CANCELLED chưa thu khi enrollment đang active")
    void findAllPending_shouldRecoverCancelledUnpaidFeeForActiveEnrollment() {
        User student = new User();
        student.setId(100L);
        student.setPhoneNumber("0900000100");
        student.setFullName("Student 100");
        Class clazz = new Class();
        clazz.setId(50L);
        clazz.setName("Class A");
        Center center = new Center();
        center.setId(CENTER_ID);
        clazz.setCenter(center);

        FeeRecord cancelledFee = FeeRecord.builder()
                .id(1L)
                .center(center)
                .studentUser(student)
                .clazz(clazz)
                .amount(java.math.BigDecimal.valueOf(1500000L))
                .paidAmount(java.math.BigDecimal.ZERO)
                .status(FeeStatus.CANCELLED)
                .build();
        ClassEnrollment activeEnrollment = new ClassEnrollment();
        activeEnrollment.setStatus(EnrollmentStatus.ACTIVE);

        when(feeRecordRepository.findAllByCenter_IdAndStatusInOrderByCreatedAtDesc(
                CENTER_ID, List.of(FeeStatus.UNPAID, FeeStatus.PARTIAL, FeeStatus.CANCELLED)))
                .thenReturn(List.of(cancelledFee));
        when(feeRecordRepository.save(cancelledFee)).thenReturn(cancelledFee);
        when(classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(50L, 100L))
                .thenReturn(Optional.of(activeEnrollment));

        List<FeeRecordResponse> responses = service.findAllPending();

        assertThat(responses).hasSize(1);
        assertThat(cancelledFee.getStatus()).isEqualTo(FeeStatus.UNPAID);
        assertThat(responses.get(0).getStatus()).isEqualTo(FeeStatus.UNPAID);
    }
}

