package com.owlexa.owlexabackend.controller;


import com.owlexa.owlexabackend.dto.request.CenterRequest;
import com.owlexa.owlexabackend.dto.response.CenterResponse;
import com.owlexa.owlexabackend.service.CenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;

    // Tạo trung tâm mới
    @PostMapping
    public CenterResponse create(@Valid @RequestBody CenterRequest request) {
        return centerService.create(request);
    }

    // Lấy toàn bộ danh sách trung tâm
    @GetMapping
    public List<CenterResponse> findAll() {
        return centerService.findAll();
    }

    // Lấy chi tiết 1 trung tâm theo id
    @GetMapping("/{id}")
    public CenterResponse findById(@PathVariable Long id) {
        return centerService.findById(id);
    }

    // Cập nhật trung tâm theo id
    @PutMapping("/{id}")
    public CenterResponse update(@PathVariable Long id, @Valid @RequestBody CenterRequest request) {
        return centerService.update(id, request);
    }

    // Xóa trung tâm theo id
    @DeleteMapping("/{id}")
    public CenterResponse delete(@PathVariable Long id) {
        return centerService.delete(id);
    }
}