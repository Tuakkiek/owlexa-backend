package com.owlexa.owlexabackend.modules.teacher_review.controller;

import com.owlexa.owlexabackend.modules.teacher_review.dto.request.TeacherReviewUpdateRequest;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewDetailResponse;
import com.owlexa.owlexabackend.modules.teacher_review.dto.response.TeacherReviewSummaryResponse;
import com.owlexa.owlexabackend.modules.teacher_review.service.TeacherReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ESSAY_GRADE')")
public class TeacherReviewController {

    private final TeacherReviewService teacherReviewService;

    @PostMapping("/submission-attempts/{attemptId}/review")
    public TeacherReviewDetailResponse createOrGetReview(@PathVariable Long attemptId) {
        return teacherReviewService.createOrGetReview(attemptId);
    }

    @GetMapping("/submission-attempts/{attemptId}/review")
    public TeacherReviewDetailResponse getReview(@PathVariable Long attemptId) {
        return teacherReviewService.getTeacherReview(attemptId);
    }

    @PutMapping("/reviews/{reviewId}")
    public TeacherReviewDetailResponse updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody TeacherReviewUpdateRequest request
    ) {
        return teacherReviewService.updateReview(reviewId, request);
    }

    @PostMapping("/reviews/{reviewId}/finalize")
    public TeacherReviewDetailResponse finalizeReview(@PathVariable Long reviewId) {
        return teacherReviewService.finalizeReview(reviewId);
    }

    @PostMapping("/reviews/{reviewId}/release")
    public TeacherReviewDetailResponse releaseReview(@PathVariable Long reviewId) {
        return teacherReviewService.releaseReview(reviewId);
    }

    @GetMapping("/assignments/{assignmentId}/reviews")
    public Page<TeacherReviewSummaryResponse> findReviewQueue(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String reviewStatus,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return teacherReviewService.findReviewQueue(assignmentId, reviewStatus, pageable);
    }
}
