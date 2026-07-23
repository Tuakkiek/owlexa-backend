package com.owlexa.owlexabackend.modules.room.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.room.dto.request.RoomRequest;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomResponse;
import com.owlexa.owlexabackend.modules.room.entity.Room;
import com.owlexa.owlexabackend.modules.room.repository.RoomRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomScheduleSummaryResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDependencyDto;

import java.util.List;


@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;


    @Transactional
    public RoomResponse create(RoomRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        if (roomRepository.existsByCodeAndCenter_Id(request.getCode().trim(), centerId)) {
            throw new DuplicateResourceException("Room code already exists in this center: " + request.getCode());
        }

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        Room room = Room.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .center(center)
                .build();

        room = roomRepository.save(room);
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> findAll() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        return roomRepository.findAllByCenter_IdAndIsActiveTrue(centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse findById(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));
        return toResponse(room);
    }

    @Transactional
    public RoomResponse update(Long roomId, RoomRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        if (!room.getCode().equalsIgnoreCase(request.getCode().trim())
                && roomRepository.existsByCodeAndCenter_Id(request.getCode().trim(), centerId)) {
            throw new DuplicateResourceException("Room code already exists in this center: " + request.getCode());
        }

        room.setCode(request.getCode().trim());
        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        room.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            room.setIsActive(request.getIsActive());
        }

        room = roomRepository.save(room);
        return toResponse(room);
    }

    @Transactional
    public void delete(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + roomId + " tại trung tâm này"));

        if (scheduleRepository.existsByRoom_IdAndCenter_Id(roomId, centerId)) {
            throw new BusinessRuleException("ROOM_IN_USE", "Phòng học " + room.getName() + " đang được sử dụng trong lịch học, không thể xóa.");
        }

        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<RoomScheduleSummaryResponse> getScheduleSummary(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        List<Schedule> schedules = scheduleRepository.findAllByRoom_IdAndCenter_Id(roomId, centerId);

        return schedules.stream()
                .map(s -> RoomScheduleSummaryResponse.builder()
                        .id(s.getId())
                        .dayOfWeek(s.getDayOfWeek().name())
                        .startTime(s.getStartTime().toString())
                        .endTime(s.getEndTime().toString())
                        .className(s.getClazz().getName())
                        .teacherName(s.getTeacherUser() != null ? s.getTeacherUser().getFullName() : "No teacher assigned")
                        .type(s.getType().name())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDeleteValidationResponse validateDelete(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        List<Schedule> schedules = scheduleRepository.findAllByRoom_IdAndCenter_Id(roomId, centerId);
        boolean canDelete = schedules.isEmpty();

        List<RoomDependencyDto> dependencies = schedules.stream()
                .map(s -> RoomDependencyDto.builder()
                        .className(s.getClazz().getName())
                        .teacherName(s.getTeacherUser() != null ? s.getTeacherUser().getFullName() : "No teacher assigned")
                        .dayOfWeek(s.getDayOfWeek().name())
                        .timeRange(s.getStartTime().toString() + "–" + s.getEndTime().toString())
                        .build())
                .toList();

        String message = canDelete ? "Room can be deleted." : "Room " + room.getName() + " cannot be deleted because it is already used by existing schedules.";

        return RoomDeleteValidationResponse.builder()
                .canDelete(canDelete)
                .message(message)
                .dependencies(dependencies)
                .build();
    }

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .capacity(room.getCapacity())
                .description(room.getDescription())
                .isActive(room.getIsActive())
                .centerId(room.getCenter().getId())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        return centerId;
    }

    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage rooms");
        }
        assertCenterMembership(currentUser, centerId);
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }
}
