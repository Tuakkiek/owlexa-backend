package com.owlexa.owlexabackend.modules.enrollment.service;

import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentAccessService {

    private final ClassEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public void requireActiveEnrollment(Long classId, Long studentUserId) {
        var enrollment = enrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Học viên không thuộc lớp này."
                ));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Học viên hiện không còn quyền làm bài của lớp."
            );
        }
    }
}
