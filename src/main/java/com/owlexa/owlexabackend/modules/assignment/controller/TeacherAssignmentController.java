package com.owlexa.owlexabackend.modules.assignment.controller;

import com.owlexa.owlexabackend.modules.assignment.dto.request.AssignmentRequest;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentStatus;
import com.owlexa.owlexabackend.modules.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ESSAY_GRADE')")
public class TeacherAssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public Page<AssignmentListResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) Long classId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return assignmentService.findAllForTeacher(search, status, classId, pageable);
    }

    @GetMapping("/{assignmentId}")
    public AssignmentDetailResponse findById(@PathVariable Long assignmentId) {
        return assignmentService.findByIdForTeacher(assignmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentDetailResponse create(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.create(request);
    }

    @PutMapping("/{assignmentId}")
    public AssignmentDetailResponse update(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return assignmentService.update(assignmentId, request);
    }

    @PostMapping("/{assignmentId}/publish")
    public AssignmentDetailResponse publish(@PathVariable Long assignmentId) {
        return assignmentService.publish(assignmentId);
    }

    @PostMapping("/{assignmentId}/close")
    public AssignmentDetailResponse close(@PathVariable Long assignmentId) {
        return assignmentService.close(assignmentId);
    }

    @PostMapping("/{assignmentId}/archive")
    public AssignmentDetailResponse archive(@PathVariable Long assignmentId) {
        return assignmentService.archive(assignmentId);
    }

    @PostMapping("/{assignmentId}/restore")
    public AssignmentDetailResponse restore(@PathVariable Long assignmentId) {
        return assignmentService.restore(assignmentId);
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long assignmentId) {
        assignmentService.delete(assignmentId);
    }
}
