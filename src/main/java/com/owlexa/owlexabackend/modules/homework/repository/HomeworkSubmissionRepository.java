package com.owlexa.owlexabackend.modules.homework.repository;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {

    @EntityGraph(attributePaths = {"homework", "questionSubmissions", "questionSubmissions.attachments", "questionSubmissions.selectedOptions", "questionSubmissions.criterionScores"})
    Optional<HomeworkSubmission> findWithDetailsByIdAndCenter_IdAndStudent_Id(Long id, Long centerId, Long studentId);

    Optional<HomeworkSubmission> findFirstByHomework_IdAndCenter_IdAndStudent_IdAndStatusOrderByAttemptNumberDesc(
            Long homeworkId, Long centerId, Long studentId, HomeworkSubmissionStatus status);

    Optional<HomeworkSubmission> findFirstByHomework_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(
            Long homeworkId, Long centerId, Long studentId);
            
    List<HomeworkSubmission> findAllByHomework_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(
            Long homeworkId, Long centerId, Long studentId);
            
    // For Epic 5: Teacher List APIs
    org.springframework.data.domain.Page<HomeworkSubmission> findAllByHomework_IdAndCenter_IdAndStatus(
            Long homeworkId, Long centerId, HomeworkSubmissionStatus status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<HomeworkSubmission> findAllByHomework_IdAndCenter_Id(
            Long homeworkId, Long centerId, org.springframework.data.domain.Pageable pageable);
            
    long countByHomework_IdAndCenter_Id(Long homeworkId, Long centerId);
    
    long countByHomework_IdAndCenter_IdAndStatusIn(Long homeworkId, Long centerId, List<HomeworkSubmissionStatus> statuses);
}
