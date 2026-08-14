package com.owlexa.owlexabackend.modules.class_management.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<Class, Long> {
    List<Class> findAllByCenter_Id(Long centerId);
    
    Optional<Class> findByIdAndCenter_Id(Long id, Long centerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Class c where c.id = :id")
    Optional<Class> findByIdForEnrollmentUpdate(@Param("id") Long id);

    long countByCenter_Id(Long centerId);

    boolean existsByNameAndCenter_Id(String name, Long centerId);

    List<Class> findAllByCourse_IdAndCenter_Id(Long courseId, Long centerId);

    boolean existsByCourse_Id(Long courseId);

    void deleteByCenter_Id(Long centerId);
}
