package com.owlexa.owlexabackend.modules.payment.dto.response;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRecordResponse {

    private Long id;
    private Long studentUserId;
    private String studentPhoneNumber;
    private String studentFullName;

    private Long centerId;
    private Long classId;
    private String className;

    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String month;
    private LocalDate dueDate;
    private FeeStatus status;

    private EnrollmentStatus enrollmentStatus;

    private Instant createdAt;
}
