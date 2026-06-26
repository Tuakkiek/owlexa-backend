package com.owlexa.owlexabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.owlexa.owlexabackend.entity.Class;

import java.util.List;

public interface ClassRepository extends JpaRepository<Class, Long> {
    List<Class> findAllByCenterId(Long centerId);

    long countByCenterId(Long centerId);

    boolean existsByNameAndCenterId(String name, Long centerId);

    void deleteByCenterId(Long centerId);
}
