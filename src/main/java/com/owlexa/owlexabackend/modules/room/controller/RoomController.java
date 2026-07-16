package com.owlexa.owlexabackend.modules.room.controller;

import com.owlexa.owlexabackend.modules.room.dto.request.RoomRequest;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomResponse;
import com.owlexa.owlexabackend.modules.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @GetMapping
    public List<RoomResponse> findAll() {
        return roomService.findAll();
    }

    @GetMapping("/{roomId}")
    public RoomResponse findById(@PathVariable Long roomId) {
        return roomService.findById(roomId);
    }

    @PutMapping("/{roomId}")
    public RoomResponse update(@PathVariable Long roomId, @Valid @RequestBody RoomRequest request) {
        return roomService.update(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long roomId) {
        roomService.delete(roomId);
    }
}
