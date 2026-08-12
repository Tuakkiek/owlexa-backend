package com.owlexa.owlexabackend.modules.enrollment.controller;

import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.modules.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/classes/{classId}/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll (
            @PathVariable Long classId,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        return enrollmentService.enroll(classId, request);
    }

    @GetMapping
    public List<EnrollmentResponse> findAllByClass(@PathVariable Long classId) {
        return enrollmentService.findAllByClass(classId);
    }

    @GetMapping("/dropped")
    public List<EnrollmentResponse> findDroppedByClass(@PathVariable Long classId) {
        return enrollmentService.findDroppedByClass(classId);
    }

    @PatchMapping("/{studentUserId}/approve")
    public EnrollmentResponse approve(
            @PathVariable Long classId,
            @PathVariable Long studentUserId
    ) {
        return enrollmentService.approve(classId, studentUserId);
    }

    @PatchMapping("/{studentUserId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable Long classId,
            @PathVariable Long studentUserId
    ) {
        enrollmentService.reject(classId, studentUserId);
    }

    @PatchMapping("/{studentUserId}/drop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop (
            @PathVariable Long classId,
            @PathVariable Long studentUserId
    ) {
        enrollmentService.drop(classId, studentUserId);
    }

    @PatchMapping("/{studentUserId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(
            @PathVariable Long classId,
            @PathVariable Long studentUserId
    ) {
        enrollmentService.suspend(classId, studentUserId);
    }

    @PatchMapping("/{studentUserId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('STUDENT_ENROLL')")
    public void reactivate(
            @PathVariable Long classId,
            @PathVariable Long studentUserId) {
        enrollmentService.reactivate(classId, studentUserId);
    }

    @PostMapping("/{studentUserId}/drop-with-reason")
    @PreAuthorize("hasAuthority('ENROLLMENT_DROP')")
    public EnrollmentResponse dropWithReason(
            @PathVariable Long classId,
            @PathVariable Long studentUserId,
            @Valid @RequestBody com.owlexa.owlexabackend.modules.enrollment.dto.request.DropEnrollmentRequest request) {
        return enrollmentService.dropWithReason(classId, studentUserId, request);
    }

    @PostMapping("/{studentUserId}/transfer")
    @PreAuthorize("hasAuthority('ENROLLMENT_TRANSFER')")
    public com.owlexa.owlexabackend.modules.enrollment.dto.response.TransferResponse transfer(
            @PathVariable Long classId,
            @PathVariable Long studentUserId,
            @Valid @RequestBody com.owlexa.owlexabackend.modules.enrollment.dto.request.TransferEnrollmentRequest request) {
        return enrollmentService.transfer(classId, studentUserId, request);
    }
}
