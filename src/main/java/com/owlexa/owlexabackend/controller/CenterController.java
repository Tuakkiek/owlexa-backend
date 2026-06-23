package com.owlexa.owlexabackend.controller;


import com.owlexa.owlexabackend.dto.request.CenterRequest;
import com.owlexa.owlexabackend.dto.response.CenterResponse;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.service.AuthorizationService;
import com.owlexa.owlexabackend.service.CenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/centers")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;
    private final AuthorizationService authorizationService;

    // CREATE
    @PostMapping
    public CenterResponse create(@Valid @RequestBody CenterRequest request) {

        if (!authorizationService.hasPermission("CENTER_CREATE") && !authorizationService.hasRole(Role.OWNER)) {
            throw new AccessDeniedException("Missing permission: CENTER_CREATE");
        }

        return centerService.create(request);
    }

    // FIND ALL
    @GetMapping
    public List<CenterResponse> findAll() {
        return centerService.findAll();
    }

    // FIND BY ID
    @GetMapping("/{id}")
    public CenterResponse findById(@PathVariable Long id) {
        return centerService.findById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public CenterResponse update(@PathVariable Long id, @Valid @RequestBody CenterRequest request) {
        return centerService.update(id, request);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        centerService.delete(id);
    }
}