package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findAllByClazzId(Long classId);

    List<ClassEnrollment> findAllByClazzIdAndCenterId(Long clazzId, Long centerId);

    Optional<ClassEnrollment> findByClazzIdAndStudentUserId(Long clazzId, Long studentUserId);

    boolean existsByClazzIdAndStudentUserId(Long clazzId, Long studentUserId);

    long countByClazzId(Long classId);
}
