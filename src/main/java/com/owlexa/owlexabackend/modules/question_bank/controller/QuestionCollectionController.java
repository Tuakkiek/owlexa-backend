package com.owlexa.owlexabackend.modules.question_bank.controller;

import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionCreateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionUpdateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionCollectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teacher/question-collections")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TEST_VIEW')")
public class QuestionCollectionController {

    private final QuestionCollectionService collectionService;

    @GetMapping
    public List<QuestionCollectionResponse> findAll() {
        return collectionService.findAll();
    }

    @GetMapping("/{collectionId}")
    public QuestionCollectionResponse findById(@PathVariable Long collectionId) {
        return collectionService.findById(collectionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionCollectionResponse create(
            @Valid @RequestBody QuestionCollectionCreateRequest request
    ) {
        return collectionService.create(request);
    }

    @PutMapping("/{collectionId}")
    public QuestionCollectionResponse update(
            @PathVariable Long collectionId,
            @Valid @RequestBody QuestionCollectionUpdateRequest request
    ) {
        return collectionService.update(collectionId, request);
    }

    @DeleteMapping("/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long collectionId) {
        collectionService.delete(collectionId);
    }
}
