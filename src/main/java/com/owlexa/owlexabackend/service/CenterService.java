package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.CenterRequest;
import com.owlexa.owlexabackend.dto.response.CenterResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    // CREATE
    @Transactional
    public CenterResponse create(CenterRequest request) {

        String subdomain = request.getSubdomain().trim().toLowerCase();

        if (centerRepository.existsBySubdomain(subdomain)) {
            throw new DuplicateResourceException(
                    "Subdomain already exists: " + request.getSubdomain()
            );
        }

        User owner = getCurrentUser();

        if (owner.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can create center");
        }

        Center center = new Center();
        center.setName(request.getName().trim());
        center.setSubdomain(subdomain);
        center.setOwner(owner);

        Center savedCenter = centerRepository.save(center);

        boolean exists = membershipRepository
                .existsByUserIdAndCenterId(owner.getId(), savedCenter.getId());

        if (!exists) {
            Membership membership = new Membership();
            membership.setUser(owner);
            membership.setCenter(savedCenter);
            membership.setJoinedByUser(owner);
            membership.setJoinedAt(Instant.now());

            membershipRepository.save(membership);
        }

        return toResponse(savedCenter);
    }

    // FIND ALL
    @Transactional(readOnly = true)
    public List<CenterResponse> findAll() {
        User owner = getCurrentUser();

        if (owner.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can view centers");
        }

        return centerRepository.findAllByOwnerId(owner.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // FIND BY ID
    @Transactional(readOnly = true)
    public CenterResponse findById(Long id) {
        User currentUser = getCurrentUser();

        Center center = centerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Center not found with id: " + id));
        assertOwnerOfCenter(currentUser, center);

        return toResponse(center);
    }

    // UPDATE
    @Transactional
    public CenterResponse update(Long id, CenterRequest request) {
        User currentUser = getCurrentUser();

        Center center = centerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Center not found with id: " + id));

        assertOwnerOfCenter(currentUser, center);

        String newName = request.getName().trim();
        String newSubdomain = request.getSubdomain()
                .trim()
                .toLowerCase();

        centerRepository.findBySubdomain(newSubdomain)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Subdomain already exists: " + request.getSubdomain()
                    );
                });

        center.setName(newName);
        center.setSubdomain(newSubdomain);

        Center savedCenter = centerRepository.save(center);

        return toResponse(savedCenter);
    }

    // DELETE
    @Transactional
    public void delete(Long id) {
        User currentUser = getCurrentUser();

        Center center = centerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Center not found with id: " + id));

        assertOwnerOfCenter(currentUser, center);

        centerRepository.delete(center);
    }
    // HELPER
    // Assert owner of center
    private void assertOwnerOfCenter(User currentUser, Center center) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage center");
        }
        if (!center.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not own this center");
        }
    }
    // Get current user
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new ResourceNotFoundException("Authentication not found");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow( () -> new ResourceNotFoundException("User not found"));
    }

    // To response
    private CenterResponse toResponse(Center center) {
        return CenterResponse.builder()
                .id(center.getId())
                .name(center.getName())
                .subdomain(center.getSubdomain())
                .createdAt(center.getCreatedAt())
                .build();
    }


}
