package com.owlexa.owlexabackend.modules.teacher_review.controller;

import com.owlexa.owlexabackend.modules.teacher_review.dto.response.StudentReviewResultResponse;
import com.owlexa.owlexabackend.modules.teacher_review.service.TeacherReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentReviewController {

    private final TeacherReviewService teacherReviewService;

    @GetMapping("/submission-attempts/{attemptId}/result")
    public StudentReviewResultResponse getReleasedResult(@PathVariable Long attemptId) {
        return teacherReviewService.getStudentReleasedResult(attemptId);
    }
}
