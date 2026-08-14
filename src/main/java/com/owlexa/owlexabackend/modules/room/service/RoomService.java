package com.owlexa.owlexabackend.modules.room.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventType;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.room.dto.request.RoomRequest;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomDependencyDto;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomResponse;
import com.owlexa.owlexabackend.modules.room.dto.response.RoomScheduleSummaryResponse;
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

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    private final ScheduleEventRepository scheduleEventRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        if (isRoomInUse(roomId, centerId)) {
            throw new BusinessRuleException("ROOM_IN_USE", "Phòng học " + room.getName() + " đang được sử dụng trong lịch học, không thể xóa.");
        }

        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<RoomScheduleSummaryResponse> getScheduleSummary(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        return findRoomUsageSummaries(roomId, centerId);
    }

    @Transactional(readOnly = true)
    public RoomDeleteValidationResponse validateDelete(Long roomId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Room room = roomRepository.findByIdAndCenter_Id(roomId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId + " in this center"));

        List<RoomDependencyDto> dependencies = findRoomBlockingDependencies(roomId, centerId);
        boolean canDelete = dependencies.isEmpty();

        String message = canDelete
                ? "Room can be deleted."
                : "Room " + room.getName() + " cannot be deleted because it is already used by schedules.";

        return RoomDeleteValidationResponse.builder()
                .canDelete(canDelete)
                .message(message)
                .dependencies(dependencies)
                .build();
    }

    private List<RoomDependencyDto> findRoomBlockingDependencies(Long roomId, Long centerId) {
        List<RoomDependencyDto> visibleScheduleDependencies = findRoomUsageSummaries(roomId, centerId).stream()
                .map(s -> RoomDependencyDto.builder()
                        .className(s.getClassName())
                        .teacherName(s.getTeacherName())
                        .source(s.getSource())
                        .dayOfWeek(s.getEventDate() != null ? s.getEventDate() : s.getDayOfWeek())
                        .timeRange(s.getStartTime() + " - " + s.getEndTime())
                        .build())
                .toList();
        List<RoomDependencyDto> ruleDependencies = scheduleRecurringRuleRepository.findAllByRoom_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(roomId, centerId).stream()
                .filter(rule -> !Boolean.FALSE.equals(rule.getIsActive()))
                .flatMap(rule -> toRuleDependencies(rule).stream())
                .toList();
        return Stream.of(visibleScheduleDependencies, ruleDependencies)
                .flatMap(List::stream)
                .toList();
    }

    private RoomResponse toResponse(Room room) {
        Long centerId = room.getCenter().getId();
        long usageCount = countRoomUsage(room.getId(), centerId);
        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .capacity(room.getCapacity())
                .description(room.getDescription())
                .isActive(room.getIsActive())
                .isInUse(usageCount > 0)
                .usageCount(usageCount)
                .centerId(centerId)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private List<RoomScheduleSummaryResponse> findRoomUsageSummaries(Long roomId, Long centerId) {
        List<RoomScheduleSummaryResponse> legacySchedules = scheduleRepository.findAllByRoom_IdAndCenter_Id(roomId, centerId).stream()
                .map(this::toLegacyScheduleSummary)
                .toList();
        List<ScheduleEvent> roomEvents = scheduleEventRepository.findAllByRoom_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(roomId, centerId);
        List<RoomScheduleSummaryResponse> events = roomEvents.stream()
                .filter(event -> event.getStatus() != ScheduleEventStatus.CANCELLED)
                .map(this::toEventSummary)
                .toList();

        return Stream.of(legacySchedules, events)
                .flatMap(List::stream)
                .sorted(this::compareSummaries)
                .toList();
    }

    private int compareSummaries(RoomScheduleSummaryResponse left, RoomScheduleSummaryResponse right) {
        String leftDate = left.getEventDate() != null ? left.getEventDate() : "";
        String rightDate = right.getEventDate() != null ? right.getEventDate() : "";
        int dateCompare = leftDate.compareTo(rightDate);
        if (dateCompare != 0) {
            return dateCompare;
        }
        int dayCompare = left.getDayOfWeek().compareTo(right.getDayOfWeek());
        if (dayCompare != 0) {
            return dayCompare;
        }
        return left.getStartTime().compareTo(right.getStartTime());
    }

    private RoomScheduleSummaryResponse toLegacyScheduleSummary(Schedule schedule) {
        return RoomScheduleSummaryResponse.builder()
                .id(schedule.getId())
                .source("LEGACY")
                .dayOfWeek(schedule.getDayOfWeek().name())
                .startTime(schedule.getStartTime().toString())
                .endTime(schedule.getEndTime().toString())
                .className(schedule.getClazz().getName())
                .teacherName(schedule.getTeacherUser() != null ? schedule.getTeacherUser().getFullName() : "Chưa phân công")
                .type(schedule.getType().name())
                .build();
    }

    private List<RoomDependencyDto> toRuleDependencies(ScheduleRecurringRule rule) {
        return parseDays(rule.getDaysOfWeek()).stream()
                .map(DayOfWeek::of)
                .map(day -> RoomDependencyDto.builder()
                        .source("RULE")
                        .className(rule.getClazz().getName())
                        .teacherName(rule.getTeacherUser() != null ? rule.getTeacherUser().getFullName() : "Chưa phân công")
                        .dayOfWeek(day.name())
                        .timeRange(rule.getStartTime() + " - " + rule.getEndTime())
                        .build())
                .toList();
    }

    private RoomScheduleSummaryResponse toEventSummary(ScheduleEvent event) {
        return RoomScheduleSummaryResponse.builder()
                .id(event.getId())
                .source("EVENT")
                .eventDate(event.getEventDate().toString())
                .dayOfWeek(event.getEventDate().getDayOfWeek().name())
                .startTime(event.getStartTime().toString())
                .endTime(event.getEndTime().toString())
                .className(event.getClazz().getName())
                .teacherName(event.getTeacherUser() != null ? event.getTeacherUser().getFullName() : "Chưa phân công")
                .type(toScheduleType(event).name())
                .build();
    }

    private ScheduleType toScheduleType(ScheduleEvent event) {
        if (event.getStatus() == ScheduleEventStatus.CANCELLED) {
            return ScheduleType.CANCELLED;
        }
        if (event.getEventType() == ScheduleEventType.EXAM) {
            return ScheduleType.EXAM;
        }
        if (event.getEventType() == ScheduleEventType.ONLINE_LESSON) {
            return ScheduleType.ONLINE_CLASS;
        }
        return ScheduleType.THEORY_CLASS;
    }

    private List<Integer> parseDays(String daysCsv) {
        if (daysCsv == null || daysCsv.isBlank()) {
            return List.of();
        }
        return Stream.of(daysCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::parseInt)
                .filter(value -> value >= 1 && value <= 7)
                .sorted()
                .toList();
    }

    private boolean isRoomInUse(Long roomId, Long centerId) {
        return scheduleRepository.existsByRoom_IdAndCenter_Id(roomId, centerId)
                || scheduleRecurringRuleRepository.existsByRoom_IdAndCenter_Id(roomId, centerId)
                || scheduleEventRepository.existsByRoom_IdAndCenter_Id(roomId, centerId);
    }

    private long countRoomUsage(Long roomId, Long centerId) {
        return findRoomUsageSummaries(roomId, centerId).size();
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
