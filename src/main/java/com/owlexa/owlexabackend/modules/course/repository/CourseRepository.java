package com.owlexa.owlexabackend.modules.course.repository;

import com.owlexa.owlexabackend.modules.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndCenter_Id(Long id, Long centerId);

    Optional<Course> findByCodeAndCenter_Id(String code, Long centerId);

    boolean existsByCodeAndCenter_Id(String code, Long centerId);

    List<Course> findAllByCenter_IdAndIsActiveTrueOrderByNameAsc(Long centerId);

    List<Course> findAllByCenter_IdOrderByNameAsc(Long centerId);

    // Legacy fallbacks
    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    List<Course> findAllByIsActiveTrue();

    List<Course> findAllByIsActiveTrueOrderByNameAsc();

    List<Course> findAllByOrderByNameAsc();
}
