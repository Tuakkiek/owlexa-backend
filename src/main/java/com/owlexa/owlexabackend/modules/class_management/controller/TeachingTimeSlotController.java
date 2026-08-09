package com.owlexa.owlexabackend.modules.class_management.controller;

import com.owlexa.owlexabackend.modules.class_management.dto.request.QuickSetupRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.TeachingTimeSlotRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.TeachingTimeSlotResponse;
import com.owlexa.owlexabackend.modules.class_management.service.TeachingTimeSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/time-slots")
public class TeachingTimeSlotController {

    private final TeachingTimeSlotService timeSlotService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SCHEDULE_VIEW', 'SCHEDULE_GENERATE', 'CLASS_VIEW', 'ROOM_VIEW')")
    public List<TeachingTimeSlotResponse> findAllForOwner() {
        return timeSlotService.findAllForOwner();
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_VIEW', 'SCHEDULE_GENERATE', 'CLASS_VIEW', 'ROOM_VIEW')")
    public List<TeachingTimeSlotResponse> findAllActiveForCurrentCenter() {
        return timeSlotService.findAllActiveForCurrentCenter();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SCHEDULE_GENERATE', 'SCHEDULE_EDIT_SINGLE', 'ROOM_MANAGE')")
    public TeachingTimeSlotResponse create(@Valid @RequestBody TeachingTimeSlotRequest request) {
        return timeSlotService.create(request);
    }

    @PostMapping("/quick-setup")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('SCHEDULE_GENERATE', 'SCHEDULE_EDIT_SINGLE', 'ROOM_MANAGE')")
    public List<TeachingTimeSlotResponse> quickSetup(@Valid @RequestBody QuickSetupRequest request) {
        return timeSlotService.quickSetup(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_GENERATE', 'SCHEDULE_EDIT_SINGLE', 'ROOM_MANAGE')")
    public TeachingTimeSlotResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TeachingTimeSlotRequest request
    ) {
        return timeSlotService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('SCHEDULE_GENERATE', 'SCHEDULE_EDIT_SINGLE', 'ROOM_MANAGE')")
    public void deleteOrDeactivate(@PathVariable Long id) {
        timeSlotService.deleteOrDeactivate(id);
    }
}
