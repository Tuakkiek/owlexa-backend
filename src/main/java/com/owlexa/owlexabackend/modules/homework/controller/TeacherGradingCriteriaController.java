package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherGradingCriteriaSaveRequest;
import com.owlexa.owlexabackend.modules.homework.dto.response.TeacherGradingCriteriaResponse;
import com.owlexa.owlexabackend.modules.homework.service.GradingCriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/grading-criteria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class TeacherGradingCriteriaController {

    private final GradingCriteriaService gradingCriteriaService;

    @GetMapping
    public ResponseEntity<Page<TeacherGradingCriteriaResponse>> getCriteriaList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(gradingCriteriaService.getCriteriaList(keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherGradingCriteriaResponse> getCriteriaById(@PathVariable Long id) {
        return ResponseEntity.ok(gradingCriteriaService.getCriteriaById(id));
    }

    @PostMapping
    public ResponseEntity<TeacherGradingCriteriaResponse> createCriteria(
            @Valid @RequestBody TeacherGradingCriteriaSaveRequest request) {
        return ResponseEntity.ok(gradingCriteriaService.createCriteria(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherGradingCriteriaResponse> updateCriteria(
            @PathVariable Long id,
            @Valid @RequestBody TeacherGradingCriteriaSaveRequest request) {
        return ResponseEntity.ok(gradingCriteriaService.updateCriteria(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCriteria(@PathVariable Long id) {
        gradingCriteriaService.deleteCriteria(id);
        return ResponseEntity.noContent().build();
    }
}
