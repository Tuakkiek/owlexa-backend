package com.owlexa.owlexabackend.modules.essay.repository;
import com.owlexa.owlexabackend.modules.essay.entity.EssaySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EssaySubmissionRepository extends JpaRepository<EssaySubmission, Long> {
    List<EssaySubmission> findAllByStudentUserIdAndCenterIdOrderByCreatedAtDesc(Long studentUserId, Long centerId);

    List<EssaySubmission> findAllByClazzIdAndCenterIdOrderByCreatedAtDesc(Long clazzId, Long centerId);
}
