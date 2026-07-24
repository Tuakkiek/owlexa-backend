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

    @EntityGraph(attributePaths = {"homeworkAssignment", "questionSubmissions", "questionSubmissions.attachments", "questionSubmissions.selectedOptions", "questionSubmissions.criterionScores"})
    Optional<HomeworkSubmission> findWithDetailsByIdAndCenter_IdAndStudent_Id(Long id, Long centerId, Long studentId);

    boolean existsByHomeworkAssignment_Id(Long homeworkAssignmentId);

    Optional<HomeworkSubmission> findFirstByHomeworkAssignment_IdAndCenter_IdAndStudent_IdAndStatusOrderByAttemptNumberDesc(
            Long homeworkAssignmentId, Long centerId, Long studentId, HomeworkSubmissionStatus status);

    Optional<HomeworkSubmission> findFirstByHomeworkAssignment_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(
            Long homeworkAssignmentId, Long centerId, Long studentId);
            
    List<HomeworkSubmission> findAllByStudent_Id(Long studentId);
    
    List<HomeworkSubmission> findAllByHomeworkAssignment_Id(Long homeworkAssignmentId);
            
    List<HomeworkSubmission> findAllByHomeworkAssignment_IdAndCenter_IdAndStudent_IdOrderByAttemptNumberDesc(
            Long homeworkAssignmentId, Long centerId, Long studentId);
            
    // For Epic 5: Teacher List APIs
    org.springframework.data.domain.Page<HomeworkSubmission> findAllByHomeworkAssignment_IdAndCenter_IdAndStatus(
            Long homeworkAssignmentId, Long centerId, HomeworkSubmissionStatus status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<HomeworkSubmission> findAllByHomeworkAssignment_IdAndCenter_Id(
            Long homeworkAssignmentId, Long centerId, org.springframework.data.domain.Pageable pageable);
            
    long countByHomeworkAssignment_IdAndCenter_Id(Long homeworkAssignmentId, Long centerId);
    
    long countByHomeworkAssignment_IdAndCenter_IdAndStatusIn(Long homeworkAssignmentId, Long centerId, List<HomeworkSubmissionStatus> statuses);

    long countByHomeworkAssignment_Id(Long homeworkAssignmentId);
    long countByHomeworkAssignment_IdAndStatus(Long homeworkAssignmentId, HomeworkSubmissionStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM HomeworkSubmission s WHERE s.homeworkAssignment.id = :assignmentId AND s.submittedAt > :dueDate")
    long countLateSubmissions(Long assignmentId, java.time.Instant dueDate);
}
