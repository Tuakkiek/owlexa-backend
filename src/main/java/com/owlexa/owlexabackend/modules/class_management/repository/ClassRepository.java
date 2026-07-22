package com.owlexa.owlexabackend.modules.class_management.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;

import java.util.List;

public interface ClassRepository extends JpaRepository<Class, Long> {
    List<Class> findAllByCenter_Id(Long centerId);

    long countByCenter_Id(Long centerId);

    boolean existsByNameAndCenter_Id(String name, Long centerId);

    List<Class> findAllByCourse_IdAndCenter_Id(Long courseId, Long centerId);

    boolean existsByCourse_Id(Long courseId);

    void deleteByCenter_Id(Long centerId);
}
