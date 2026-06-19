package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.FeeStatus;
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
    private String month;
    private LocalDate dueDate;
    private FeeStatus status;

    private Instant createdAt;
}