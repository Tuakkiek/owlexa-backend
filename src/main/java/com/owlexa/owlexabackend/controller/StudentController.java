package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.BulkStudentRequest;
import com.owlexa.owlexabackend.dto.request.StudentRequest;
import com.owlexa.owlexabackend.dto.response.BulkStudentResult;
import com.owlexa.owlexabackend.dto.response.StudentResponse;
import com.owlexa.owlexabackend.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public List<StudentResponse> findAll() {
        return studentService.findAll();
    }

    // Create one
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }

    // Update
    @PutMapping("/{studentId}")
    public StudentResponse update(@PathVariable Long studentId,@Valid @RequestBody StudentRequest request) {
        return studentService.update(studentId, request);
    }

    // Delete
    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long studentId) {
        studentService.delete(studentId);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BulkStudentResult> bulkCreate(@RequestBody BulkStudentRequest request) {
        return studentService.bulkCreate(request);
    }
}
