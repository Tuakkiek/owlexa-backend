package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentProgressResponse;
import com.owlexa.owlexabackend.modules.homework.service.StudentProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/homework-progress")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentProgressController {

    private final StudentProgressService studentProgressService;

    @GetMapping
    public StudentProgressResponse getProgress(
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentProgressService.getStudentProgress(studentId);
    }
}
