package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherHomeworkAssignmentSaveRequest;
import com.owlexa.owlexabackend.modules.homework.service.TeacherHomeworkAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/homework-assignments")
@PreAuthorize("hasRole('TEACHER')")
@RequiredArgsConstructor
public class TeacherHomeworkAssignmentController {

    private final TeacherHomeworkAssignmentService assignmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void assignHomework(
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkAssignmentSaveRequest request) {
        assignmentService.assignHomework(teacherId, request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkAssignmentSaveRequest request) {
        assignmentService.updateAssignment(teacherId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.deleteAssignment(id, teacherId);
    }

    @PostMapping("/{id}/schedule")
    public void scheduleAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.scheduleAssignment(teacherId, id);
    }

    @PostMapping("/{id}/release-grades")
    public void releaseGrades(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.releaseGrades(id, teacherId);
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.closeAssignment(teacherId, id);
    }
    
    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.cancelAssignment(teacherId, id);
    }
    
    @PostMapping("/{id}/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        assignmentService.reopenAssignment(teacherId, id);
    }

    @GetMapping("/upcoming")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment> getUpcomingAssignments(
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return assignmentService.getDashboardAssignments(teacherId, "UPCOMING");
    }

    @GetMapping("/open")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment> getOpenAssignments(
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return assignmentService.getDashboardAssignments(teacherId, "OPEN");
    }

    @GetMapping("/closing-soon")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment> getClosingSoonAssignments(
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return assignmentService.getDashboardAssignments(teacherId, "CLOSING_SOON");
    }

    @GetMapping("/recently-closed")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment> getRecentlyClosedAssignments(
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return assignmentService.getDashboardAssignments(teacherId, "RECENTLY_CLOSED");
    }

    @GetMapping("/drafts")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment> getDraftAssignments(
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return assignmentService.getDashboardAssignments(teacherId, "DRAFTS");
    }

    @GetMapping("/library")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkAssignmentResponse> getAssignmentLibrary(
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus status,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type) {
        return assignmentService.searchAssignments(teacherId, keyword, classId, status, type);
    }
}
