package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkTemplate;
import com.owlexa.owlexabackend.modules.homework.service.OwnerHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/homework-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerHomeworkTemplateController {

    private final OwnerHomeworkService ownerHomeworkService;

    // We simply return entities here for demonstration, ideally DTOs should be used.
    @GetMapping
    public org.springframework.data.domain.Page<HomeworkTemplate> getAllTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty difficulty,
            org.springframework.data.domain.Pageable pageable) {
        return ownerHomeworkService.getAllTemplates(keyword, type, difficulty, pageable);
    }

    @GetMapping("/{id}")
    public HomeworkTemplate getTemplateDetail(@PathVariable Long id) {
        return ownerHomeworkService.getTemplateDetail(id);
    }
}
