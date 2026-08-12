package com.owlexa.owlexabackend.modules.enrollment.dto.response;
import com.owlexa.owlexabackend.modules.enrollment.entity.DropReason;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
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
    private EnrollmentStatus status;
    private Instant enrolledAt;

    // Drop fields
    private DropReason dropReason;
    private Instant droppedAt;

    // Transfer fields
    private Long transferredToEnrollmentId;
    private Long transferredFromEnrollmentId;
}
