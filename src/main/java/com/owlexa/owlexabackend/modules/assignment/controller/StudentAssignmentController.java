package com.owlexa.owlexabackend.modules.assignment.controller;

import com.owlexa.owlexabackend.modules.assignment.dto.response.StudentAssignmentListResponse;
import com.owlexa.owlexabackend.modules.assignment.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/student/assignments")
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public List<StudentAssignmentListResponse> findAll() {
        return assignmentService.findAllForStudent();
    }
}
