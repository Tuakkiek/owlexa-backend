package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.GradingCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradingCriteriaRepository extends JpaRepository<GradingCriteria, Long> {

    @Query("SELECT g FROM GradingCriteria g WHERE " +
            "g.center.id = :centerId AND " +
            "g.archived = :archived AND " +
            "(:keyword IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<GradingCriteria> findByCenterIdAndArchivedAndKeyword(
            @Param("centerId") Long centerId,
            @Param("archived") boolean archived,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
