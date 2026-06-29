package com.owlexa.owlexabackend.modules.essay.repository;
import com.owlexa.owlexabackend.modules.essay.entity.EssayRubric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EssayRubricRepository extends JpaRepository<EssayRubric, Long> {
    List<EssayRubric> findAllByCreatedByUserIdAndCenterId(Long createdByUserId, Long centerId);
}
