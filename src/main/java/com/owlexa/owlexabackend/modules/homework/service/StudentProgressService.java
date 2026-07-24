package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentProgressResponse;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkSubmissionStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkSubmissionRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProgressService {

    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkAssignmentRepository assignmentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    private double calculateTotalScore(HomeworkSubmission s) {
        double total = 0.0;
        if (s.getQuestionSubmissions() != null) {
            for (var qs : s.getQuestionSubmissions()) {
                if (qs.getTeacherOverrideScore() != null) total += qs.getTeacherOverrideScore();
                else if (qs.getScore() != null) total += qs.getScore();
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgress(Long studentId) {
        Long centerId = TenantContext.getCurrentTenantId();

        List<HomeworkSubmission> allSubmissions = submissionRepository.findAllByStudent_Id(studentId);

        // 1. Get all graded submissions for the student
        List<HomeworkSubmission> gradedSubmissions = allSubmissions.stream()
                .filter(s -> s.getStatus() == HomeworkSubmissionStatus.GRADED)
                .collect(Collectors.toList());

        // Calculate Average Score (normalized to percentage)
        double totalPercentage = 0.0;
        List<StudentProgressResponse.ScoreHistoryItem> scoreHistory = gradedSubmissions.stream()
                .sorted(Comparator.comparing(HomeworkSubmission::getSubmittedAt))
                .map(sub -> {
                    double score = calculateTotalScore(sub);
                    double maxScore = sub.getHomeworkAssignment().getHomeworkTemplate().getMaxScore();
                    return StudentProgressResponse.ScoreHistoryItem.builder()
                            .assignmentId(sub.getHomeworkAssignment().getId())
                            .assignmentTitle(sub.getHomeworkAssignment().getHomeworkTemplate().getTitle())
                            .score(score)
                            .maxScore(maxScore)
                            .submittedAt(sub.getSubmittedAt())
                            .build();
                })
                .collect(Collectors.toList());

        for (StudentProgressResponse.ScoreHistoryItem item : scoreHistory) {
            if (item.getMaxScore() > 0) {
                totalPercentage += (item.getScore() / item.getMaxScore()) * 100.0;
            }
        }
        
        double averageScore = scoreHistory.isEmpty() ? 0.0 : totalPercentage / scoreHistory.size();

        // 2. Count metrics
        // Completed = submitted (including graded, needs grading)
        int totalCompleted = allSubmissions.stream()
                .filter(s -> s.getStatus() != HomeworkSubmissionStatus.IN_PROGRESS)
                .mapToInt(e -> 1).sum();

        // Missing = assignments that are OPEN or CLOSED but student has no non-IN_PROGRESS submission, and past due date
        List<Long> enrolledClassIds = enrollmentRepository.findAllByStudentUser_IdAndCenter_Id(studentId, centerId).stream()
                .filter(e -> e.getStatus() == com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE)
                .map(e -> e.getClazz().getId())
                .collect(Collectors.toList());

        List<HomeworkAssignment> allAssignmentsForStudent = assignmentRepository.findAllByCenter_Id(centerId).stream()
                .filter(a -> enrolledClassIds.contains(a.getClazz().getId()))
                .collect(Collectors.toList());

        int totalMissing = 0;
        int totalLate = 0;
        java.time.Instant now = java.time.Instant.now();

        for (HomeworkAssignment assignment : allAssignmentsForStudent) {
            boolean hasSubmitted = allSubmissions.stream()
                    .anyMatch(s -> s.getHomeworkAssignment().getId().equals(assignment.getId()) && s.getStatus() != HomeworkSubmissionStatus.IN_PROGRESS);
            
            if (!hasSubmitted && assignment.getDueDate() != null && assignment.getDueDate().isBefore(now)) {
                totalMissing++;
            }
            
            // Check if submitted late
            if (hasSubmitted && assignment.getDueDate() != null) {
                HomeworkSubmission sub = allSubmissions.stream()
                        .filter(s -> s.getHomeworkAssignment().getId().equals(assignment.getId()) && s.getStatus() != HomeworkSubmissionStatus.IN_PROGRESS)
                        .findFirst().orElse(null);
                if (sub != null && sub.getSubmittedAt() != null && sub.getSubmittedAt().isAfter(assignment.getDueDate())) {
                    totalLate++;
                }
            }
        }

        return StudentProgressResponse.builder()
                .averageScore(averageScore)
                .totalCompleted(totalCompleted)
                .totalMissing(totalMissing)
                .totalLate(totalLate)
                .scoreHistory(scoreHistory)
                .build();
    }
}
