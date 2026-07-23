package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkDetailResponse;
import com.owlexa.owlexabackend.modules.homework.dto.response.student.StudentHomeworkListResponse;
import com.owlexa.owlexabackend.modules.homework.service.StudentHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student/homeworks")
@RequiredArgsConstructor
public class StudentHomeworkController {

    private final StudentHomeworkService studentHomeworkService;

    @GetMapping
    public List<StudentHomeworkListResponse> getMyHomeworks(
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentHomeworkService.getMyHomeworks(studentId);
    }

    @GetMapping("/{id}")
    public StudentHomeworkDetailResponse getHomeworkDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long studentId) {
        return studentHomeworkService.getHomeworkDetails(studentId, id);
    }
}
