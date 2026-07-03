package com.owlexa.owlexabackend.modules.essay.repository;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EssaySubmissionRepository extends JpaRepository<EssaySubmission, Long> {
    List<EssaySubmission> findAllByStudentUser_IdAndCenter_IdOrderByCreatedAtDesc(Long studentUserId, Long centerId);

    List<EssaySubmission> findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(Long clazzId, Long centerId);
}
