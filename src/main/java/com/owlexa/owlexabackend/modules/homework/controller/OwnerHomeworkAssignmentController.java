package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.service.OwnerHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/homework-assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerHomeworkAssignmentController {

    private final OwnerHomeworkService ownerHomeworkService;

    @GetMapping
    public org.springframework.data.domain.Page<HomeworkAssignment> getAllAssignments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus status,
            org.springframework.data.domain.Pageable pageable) {
        return ownerHomeworkService.getAllAssignments(keyword, classId, teacherId, status, pageable);
    }

    @GetMapping("/{id}")
    public HomeworkAssignment getAssignmentDetail(@PathVariable Long id) {
        return ownerHomeworkService.getAssignmentDetail(id);
    }
}
