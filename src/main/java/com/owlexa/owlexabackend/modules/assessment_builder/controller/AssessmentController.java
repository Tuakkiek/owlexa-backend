package com.owlexa.owlexabackend.modules.assessment_builder.controller;

import com.owlexa.owlexabackend.modules.assessment_builder.dto.request.AssessmentRequest;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentDetailResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.dto.response.AssessmentListResponse;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentStatus;
import com.owlexa.owlexabackend.modules.assessment_builder.service.AssessmentService;
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
@RequestMapping("/teacher/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TEST_VIEW')")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    public Page<AssessmentListResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AssessmentStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return assessmentService.findAll(search, status, pageable);
    }

    @GetMapping("/{assessmentId}")
    public AssessmentDetailResponse findById(@PathVariable Long assessmentId) {
        return assessmentService.findById(assessmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentDetailResponse create(@Valid @RequestBody AssessmentRequest request) {
        return assessmentService.create(request);
    }

    @PutMapping("/{assessmentId}")
    public AssessmentDetailResponse update(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentRequest request
    ) {
        return assessmentService.update(assessmentId, request);
    }

    @PostMapping("/{assessmentId}/publish")
    public AssessmentDetailResponse publish(@PathVariable Long assessmentId) {
        return assessmentService.publish(assessmentId);
    }

    @PostMapping("/{assessmentId}/archive")
    public AssessmentDetailResponse archive(@PathVariable Long assessmentId) {
        return assessmentService.archive(assessmentId);
    }

    @DeleteMapping("/{assessmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long assessmentId) {
        assessmentService.delete(assessmentId);
    }
}
