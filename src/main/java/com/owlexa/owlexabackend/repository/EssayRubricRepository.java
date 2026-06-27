package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.EssayRubric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EssayRubricRepository extends JpaRepository<EssayRubric, Long> {
    List<EssayRubric> findAllByCreatedByUserIdAndCenterId(Long createdByUserId, Long centerId);
}
