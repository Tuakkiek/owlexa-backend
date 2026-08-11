package com.owlexa.owlexabackend.modules.teacher.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSalaryResponse {

    private Long teacherUserId;
    private Long centerId;
    private String teacherFullName;
    private String teacherPhoneNumber;

    /**
     * Mức lương hiện tại. Null nếu OWNER chưa set.
     */
    private BigDecimal salary;

    private String currency;

    private Instant createdAt;
    private Instant updatedAt;
}