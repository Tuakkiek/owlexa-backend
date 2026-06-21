package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.ClassEnrollment;
import com.owlexa.owlexabackend.entity.EnrollmentStatus;
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

    void deleteByCenterId(Long centerId);
}
