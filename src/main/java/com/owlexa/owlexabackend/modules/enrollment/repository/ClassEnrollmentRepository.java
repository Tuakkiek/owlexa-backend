package com.owlexa.owlexabackend.modules.enrollment.repository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findAllByClazz_IdAndStatus(Long classId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByClazz_IdAndStatusIn(Long classId, List<EnrollmentStatus> statuses);

    List<ClassEnrollment> findAllByClazz_IdAndCenter_Id(Long clazzId, Long centerId);

    Optional<ClassEnrollment> findByClazz_IdAndStudentUser_Id(Long clazzId, Long studentUserId);

    boolean existsByClazz_IdAndStudentUser_Id(Long clazzId, Long studentUserId);

    long countByClazz_IdAndStatus(Long classId, EnrollmentStatus status);

    long countByClazz_IdAndStatusIn(Long classId, List<EnrollmentStatus> statuses);

    boolean existsByClazz_IdAndStudentUser_IdAndStatus(Long classId, Long studentUserId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByStudentUser_IdAndCenter_Id(Long studentUserId, Long centerId);

    List<ClassEnrollment> findAllByStudentUser_IdAndCenter_IdAndStatusIn(
            Long studentUserId, Long centerId, List<EnrollmentStatus> statuses);

    void deleteByCenter_Id(Long centerId);
}
