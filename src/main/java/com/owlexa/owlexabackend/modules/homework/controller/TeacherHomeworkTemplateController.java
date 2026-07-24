package com.owlexa.owlexabackend.modules.homework.controller;

import com.owlexa.owlexabackend.modules.homework.dto.request.TeacherHomeworkTemplateSaveRequest;
import com.owlexa.owlexabackend.modules.homework.service.TeacherHomeworkTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/homework-templates")
@RequiredArgsConstructor
public class TeacherHomeworkTemplateController {

    private final TeacherHomeworkTemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createTemplate(
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkTemplateSaveRequest request) {
        templateService.saveHomeworkTemplate(teacherId, null, request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @Valid @RequestBody TeacherHomeworkTemplateSaveRequest request) {
        templateService.saveHomeworkTemplate(teacherId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        templateService.deleteHomeworkTemplate(id, teacherId);
    }
    
    @PostMapping("/{id}/duplicate")
    public Long duplicateTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long teacherId) {
        return templateService.duplicateHomeworkTemplate(id, teacherId);
    }

    @GetMapping("/library")
    public java.util.List<com.owlexa.owlexabackend.modules.homework.dto.response.HomeworkTemplateResponse> getTemplateLibrary(
            @AuthenticationPrincipal(expression = "id") Long teacherId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkType type,
            @RequestParam(required = false) com.owlexa.owlexabackend.modules.homework.enums.HomeworkDifficulty difficulty,
            @RequestParam(required = false, defaultValue = "false") Boolean archived) {
        return templateService.searchTemplates(teacherId, keyword, type, difficulty, archived);
    }
}
