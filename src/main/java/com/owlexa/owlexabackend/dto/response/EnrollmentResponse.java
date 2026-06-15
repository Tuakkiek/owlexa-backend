package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long classId;
    private Long centerId;

    private Long studentUserId;
    private String studentPhoneNumber;
    private String studentFullName;

    private Long enrollmentByUserId;
    private Instant enrolledAt;
}
