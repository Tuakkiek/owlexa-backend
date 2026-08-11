package com.owlexa.owlexabackend.modules.course.controller;

import com.owlexa.owlexabackend.modules.course.dto.request.CourseRequest;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseStatisticsResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseClassResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @GetMapping
    public List<CourseResponse> findAll() {
        return courseService.findAll();
    }

    @GetMapping("/all")
    public List<CourseResponse> findAllIncludingInactive() {
        return courseService.findAllIncludingInactive();
    }

    @GetMapping("/{courseId}")
    public CourseResponse findById(@PathVariable Long courseId) {
        return courseService.findById(courseId);
    }

    @GetMapping("/{courseId}/statistics")
    public CourseStatisticsResponse getStatistics(@PathVariable Long courseId) {
        return courseService.getStatistics(courseId);
    }

    @GetMapping("/{courseId}/classes")
    public List<CourseClassResponse> getClasses(@PathVariable Long courseId) {
        return courseService.getClasses(courseId);
    }

    @GetMapping("/{courseId}/delete-validation")
    public CourseDeleteValidationResponse validateDelete(@PathVariable Long courseId) {
        return courseService.validateDelete(courseId);
    }

    @PutMapping("/{courseId}")
    public CourseResponse update(@PathVariable Long courseId, @Valid @RequestBody CourseRequest request) {
        return courseService.update(courseId, request);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long courseId) {
        courseService.delete(courseId);
    }
}
