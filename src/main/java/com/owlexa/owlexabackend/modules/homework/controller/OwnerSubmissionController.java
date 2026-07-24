package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkSubmission;
import com.owlexa.owlexabackend.modules.homework.service.OwnerHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner/homework-assignments/{assignmentId}/submissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerSubmissionController {

    private final OwnerHomeworkService ownerHomeworkService;

    @GetMapping
    public Page<HomeworkSubmission> getSubmissionsForAssignment(
            @PathVariable Long assignmentId,
            Pageable pageable) {
        return ownerHomeworkService.getSubmissionsForAssignment(assignmentId, pageable);
    }
}
