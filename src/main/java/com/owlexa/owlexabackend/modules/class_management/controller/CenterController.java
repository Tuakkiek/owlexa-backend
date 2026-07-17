package com.owlexa.owlexabackend.modules.class_management.controller;
import com.owlexa.owlexabackend.modules.class_management.dto.request.CenterRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.CenterResponse;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.class_management.service.CenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('CENTER_SETTINGS_UPDATE')")
    public CenterResponse create(@Valid @RequestBody CenterRequest request) {
        return centerService.create(request);
    }

    // FIND ALL
    @GetMapping
    @PreAuthorize("hasAuthority('CENTER_VIEW')")
    public List<CenterResponse> findAll() {
        return centerService.findAll();
    }

    // FIND BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTER_VIEW')")
    public CenterResponse findById(@PathVariable Long id) {
        return centerService.findById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTER_SETTINGS_UPDATE')")
    @com.owlexa.owlexabackend.common.audit.AuditLog(action = "UPDATE_CENTER")
    public CenterResponse update(@PathVariable Long id, @Valid @RequestBody CenterRequest request) {
        return centerService.update(id, request);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTER_SETTINGS_UPDATE')")
    @com.owlexa.owlexabackend.common.audit.AuditLog(action = "DELETE_CENTER")
    public void delete(@PathVariable Long id) {
        centerService.delete(id);
    }
}