package com.owlexa.owlexabackend.modules.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.dto.request.CenterRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.CenterResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.attendance.repository.AttendanceRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;

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
    private final AttendanceRepository attendanceRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final ScheduleRepository scheduleRepository;

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

        if (!centerRepository.findAllByOwner_Id(owner.getId()).isEmpty()) {
            throw new BadRequestException(
                    "Chủ sở hữu đã có trung tâm. Mỗi tài khoản Chủ sở hữu chỉ được quản lý 1 trung tâm."
            );
        }

        Center center = new Center();
        center.setName(request.getName().trim());
        center.setSubdomain(subdomain);
        center.setOwner(owner);

        Center savedCenter = centerRepository.save(center);

        boolean exists = membershipRepository
                .existsByUser_IdAndCenter_Id(owner.getId(), savedCenter.getId());

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

        return centerRepository.findAllByOwner_Id(owner.getId()).stream()
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

        throw new BadRequestException("Không thể xóa trung tâm duy nhất của chủ sở hữu.");
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
