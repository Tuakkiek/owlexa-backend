package com.owlexa.owlexabackend.modules.grading_criteria.controller;

import com.owlexa.owlexabackend.modules.grading_criteria.dto.request.GradingCriteriaRequest;
import com.owlexa.owlexabackend.modules.grading_criteria.dto.response.GradingCriteriaResponse;
import com.owlexa.owlexabackend.modules.grading_criteria.service.GradingCriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/teacher/grading-criteria")
@RequiredArgsConstructor
public class GradingCriteriaController {

    private final GradingCriteriaService gradingCriteriaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TEACHER_GRADING_CRITERIA', 'TEACHER_QUESTION_BANK')")
    public List<GradingCriteriaResponse> findAll(@RequestParam(required = false) String search) {
        return gradingCriteriaService.findAll(search);
    }

    @GetMapping("/{criteriaId}")
    @PreAuthorize("hasAnyAuthority('TEACHER_GRADING_CRITERIA', 'TEACHER_QUESTION_BANK')")
    public GradingCriteriaResponse findById(@PathVariable Long criteriaId) {
        return gradingCriteriaService.findById(criteriaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEACHER_GRADING_CRITERIA')")
    public GradingCriteriaResponse create(@Valid @RequestBody GradingCriteriaRequest request) {
        return gradingCriteriaService.create(request);
    }

    @PutMapping("/{criteriaId}")
    @PreAuthorize("hasAuthority('TEACHER_GRADING_CRITERIA')")
    public GradingCriteriaResponse update(
            @PathVariable Long criteriaId,
            @Valid @RequestBody GradingCriteriaRequest request
    ) {
        return gradingCriteriaService.update(criteriaId, request);
    }

    @DeleteMapping("/{criteriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TEACHER_GRADING_CRITERIA')")
    public void delete(@PathVariable Long criteriaId) {
        gradingCriteriaService.delete(criteriaId);
    }
}
