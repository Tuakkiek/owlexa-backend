package com.owlexa.owlexabackend.modules.teacher.controller;

import com.owlexa.owlexabackend.modules.teacher.dto.request.TeacherSalaryRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherSalaryResponse;
import com.owlexa.owlexabackend.modules.teacher.service.TeacherSalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API cho salary của TEACHER.
 *
 * Theo auth-roles-plan.md section 6.2:
 * - GET  /owner/teachers/{teacherId}/salary  → lấy salary hiện tại
 * - PUT  /owner/teachers/{teacherId}/salary  → set/update salary
 *
 * Bổ sung thêm:
 * - DELETE /owner/teachers/{teacherId}/salary → xóa salary (set về null)
 *   để OWNER có thể "undo" nếu set nhầm, thay vì phải tạo API mới.
 *
 * Tất cả endpoint đều:
 * - Yêu cầu role OWNER (SecurityConfig đã enforce ở /owner/**)
 * - Yêu cầu X-Tenant-ID header (filter đã set TenantContext)
 * - Yêu cầu OWNER là thành viên của center hiện tại (service kiểm tra)
 */
@RestController
@RequestMapping("/owner/teachers/{teacherId}/salary")
@RequiredArgsConstructor
public class TeacherSalaryController {

    private final TeacherSalaryService teacherSalaryService;

    @GetMapping
    public TeacherSalaryResponse get(@PathVariable Long teacherId) {
        return teacherSalaryService.get(teacherId);
    }

    @PutMapping
    public TeacherSalaryResponse upsert(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherSalaryRequest request
    ) {
        return teacherSalaryService.upsert(teacherId, request);
    }

    @DeleteMapping
    public TeacherSalaryResponse clear(@PathVariable Long teacherId) {
        return teacherSalaryService.clear(teacherId);
    }
}