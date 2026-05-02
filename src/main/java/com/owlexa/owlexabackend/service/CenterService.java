package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.response.CenterResponse;
import com.owlexa.owlexabackend.dto.request.CenterRequest;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.repository.CenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;

    // Create
    public CenterResponse create(CenterRequest request) {
        if (centerRepository.existsBySubdomain(request.getSubdomain())) {
            throw new DuplicateResourceException("Subdomain already exists: " + request.getSubdomain());
        }

        Center center = new Center();
        center.setName(request.getName());
        center.setSubdomain(request.getSubdomain());

        return toResponse(centerRepository.save(center));
    }
    // Find all
    public List<CenterResponse> findAll() {
        return centerRepository.findAll().stream()
                .map(center -> toResponse(center))
                .toList();
    }
    // Find by id
    public CenterResponse findById(Long id) {
        Center center = centerRepository.findById(id)
                .orElseThrow( () -> new RuntimeException("Center not found with id: " + id));
        return toResponse(center);
    }
    // Update
    public CenterResponse update(Long id, CenterRequest request) {
        Center center = centerRepository.findById(id)
                .orElseThrow( () -> new RuntimeException("Center not found with id: " + id));

        centerRepository.findBySubdomain(request.getSubdomain())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Subdomain already exists: " + request.getSubdomain());
                });

        center.setName(request.getName());
        center.setSubdomain(request.getSubdomain());

        return toResponse(centerRepository.save(center));
    }
    // Delete
    public CenterResponse delete(Long id) {
        Center center = centerRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Center not found with id: " + id));

        centerRepository.delete(center);
        return toResponse(center);
    }

    // Map CenterResponse
    private CenterResponse toResponse(Center center) {
        return CenterResponse.builder()
                .id(center.getId())
                .name(center.getName())
                .subdomain(center.getSubdomain())
                .createdAt(center.getCreatedAt())
                .build();
    }
}
