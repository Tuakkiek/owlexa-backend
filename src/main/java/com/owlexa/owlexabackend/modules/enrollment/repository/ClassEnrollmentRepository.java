package com.owlexa.owlexabackend.modules.enrollment.repository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findAllByClazz_IdAndStatus(Long classId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByClazz_IdAndStatusIn(Long classId, List<EnrollmentStatus> statuses);

    List<ClassEnrollment> findAllByClazz_IdAndCenter_Id(Long clazzId, Long centerId);

    List<ClassEnrollment> findAllByClazz_IdAndStudentUser_IdOrderByIdDesc(Long clazzId, Long studentUserId);

    /**
     * Compatibility API used by older services. Selecting from the list keeps
     * legacy duplicate rows from surfacing as IncorrectResultSizeDataAccessException.
     */
    default Optional<ClassEnrollment> findByClazz_IdAndStudentUser_Id(Long clazzId, Long studentUserId) {
        return findAllByClazz_IdAndStudentUser_IdOrderByIdDesc(clazzId, studentUserId).stream()
                .sorted(Comparator
                        .comparingInt((ClassEnrollment enrollment) -> statusPriority(enrollment.getStatus()))
                        .thenComparing(ClassEnrollment::getId, Comparator.reverseOrder()))
                .findFirst();
    }

    private static int statusPriority(EnrollmentStatus status) {
        return switch (status) {
            case ACTIVE -> 0;
            case SUSPENDED -> 1;
            case PENDING -> 2;
            case DROPPED -> 3;
        };
    }

    boolean existsByClazz_IdAndStudentUser_Id(Long clazzId, Long studentUserId);

    long countByClazz_IdAndStatus(Long classId, EnrollmentStatus status);

    long countByClazz_IdAndStatusIn(Long classId, List<EnrollmentStatus> statuses);

    boolean existsByClazz_IdAndStudentUser_IdAndStatus(Long classId, Long studentUserId, EnrollmentStatus status);

    boolean existsByStudentUser_IdAndClazz_TeacherUser_IdAndCenter_IdAndStatus(
            Long studentUserId,
            Long teacherUserId,
            Long centerId,
            EnrollmentStatus status);

    void deleteByClazz_IdAndCenter_Id(Long classId, Long centerId);

    List<ClassEnrollment> findAllByStudentUser_IdAndCenter_Id(Long studentUserId, Long centerId);

    List<ClassEnrollment> findAllByStudentUser_IdAndCenter_IdAndStatusIn(
            Long studentUserId, Long centerId, List<EnrollmentStatus> statuses);

    void deleteByCenter_Id(Long centerId);
}
