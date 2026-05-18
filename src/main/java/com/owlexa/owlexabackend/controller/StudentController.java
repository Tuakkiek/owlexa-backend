package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.BulkStudentRequest;
import com.owlexa.owlexabackend.dto.response.BulkStudentResult;
import com.owlexa.owlexabackend.dto.response.StudentResponse;
import com.owlexa.owlexabackend.service.StudentService;
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

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BulkStudentResult> bulkCreate(@RequestBody BulkStudentRequest request) {
        return studentService.bulkCreate(request);
    }
}
