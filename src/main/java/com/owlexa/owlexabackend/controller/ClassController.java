package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.ClassRequest;
import com.owlexa.owlexabackend.dto.response.ClassResponse;
import com.owlexa.owlexabackend.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    // Create
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse create(@Valid @RequestBody ClassRequest request) {
        return classService.create(request);
    }
    // Find all
    @GetMapping
    public List<ClassResponse> findAll() {
        return classService.findAll();
    }
    // Update
    @PutMapping("/{classId}")
    public ClassResponse update(@PathVariable Long classId, @RequestBody ClassRequest request) {
        return classService.update(classId, request);
    }
    // Delete
    @DeleteMapping("/{classId}")
    public void delete(@PathVariable Long classId) {
        classService.delete(classId);
    }
}
