package com.owlexa.owlexabackend.modules.enrollment.repository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findAllByClazzIdAndStatus(Long classId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByClazzIdAndCenterId(Long clazzId, Long centerId);

    Optional<ClassEnrollment> findByClazzIdAndStudentUserId(Long clazzId, Long studentUserId);

    boolean existsByClazzIdAndStudentUserId(Long clazzId, Long studentUserId);

    long countByClazzIdAndStatus(Long classId, EnrollmentStatus status);

    boolean existsByClazzIdAndStudentUserIdAndStatus(Long classId, Long studentUserId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByStudentUserIdAndCenterId(Long studentUserId, Long centerId);

    void deleteByCenterId(Long centerId);
}
