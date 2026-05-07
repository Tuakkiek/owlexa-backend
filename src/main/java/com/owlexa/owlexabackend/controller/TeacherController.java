package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.BulkTeacherRequest;
import com.owlexa.owlexabackend.dto.request.TeacherRequest;
import com.owlexa.owlexabackend.dto.response.BulkTeacherResult;
import com.owlexa.owlexabackend.dto.response.TeacherResponse;
import com.owlexa.owlexabackend.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse create(@Valid @RequestBody TeacherRequest request) {
        return teacherService.create(request);
    }

    @GetMapping
    public List<TeacherResponse> findAll() {
        return teacherService.findAll();
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BulkTeacherResult> bulkCreate(@RequestBody BulkTeacherRequest request) {
        return teacherService.bulkCreate(request);
    }
}
