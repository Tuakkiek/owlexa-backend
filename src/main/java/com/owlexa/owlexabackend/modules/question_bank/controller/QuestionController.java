package com.owlexa.owlexabackend.modules.question_bank.controller;

import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public Page<QuestionResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) QuestionDifficulty difficulty,
            @RequestParam(required = false) Long gradingCriteriaId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return questionService.findAll(search, type, difficulty, gradingCriteriaId, pageable);
    }

    @GetMapping("/{questionId}")
    public QuestionResponse findById(@PathVariable Long questionId) {
        return questionService.findById(questionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) {
        return questionService.create(request);
    }

    @PutMapping("/{questionId}")
    public QuestionResponse update(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request
    ) {
        return questionService.update(questionId, request);
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long questionId) {
        questionService.delete(questionId);
    }
}
