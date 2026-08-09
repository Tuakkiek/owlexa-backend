package com.owlexa.owlexabackend.modules.class_management.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.dto.request.QuickSetupRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.request.TeachingTimeSlotRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.TeachingTimeSlotResponse;
import com.owlexa.owlexabackend.modules.class_management.entity.TeachingTimeSlot;
import com.owlexa.owlexabackend.modules.class_management.entity.TimeSlotPeriod;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.TeachingTimeSlotRepository;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeachingTimeSlotService {

    private final TeachingTimeSlotRepository timeSlotRepository;
    private final ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<TeachingTimeSlotResponse> findAllForOwner() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return timeSlotRepository.findAllByCenter_IdOrderByDisplayOrderAscStartTimeAsc(centerId)
                .stream()
                .map(slot -> toResponse(slot, centerId))
                .sorted(Comparator.comparing(TeachingTimeSlotResponse::getPeriod)
                        .thenComparing(TeachingTimeSlotResponse::getDisplayOrder)
                        .thenComparing(TeachingTimeSlotResponse::getStartTime))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeachingTimeSlotResponse> findAllActiveForCurrentCenter() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        return timeSlotRepository.findAllByCenter_IdAndIsActiveTrueOrderByDisplayOrderAscStartTimeAsc(centerId)
                .stream()
                .map(slot -> toResponse(slot, centerId))
                .sorted(Comparator.comparing(TeachingTimeSlotResponse::getPeriod)
                        .thenComparing(TeachingTimeSlotResponse::getDisplayOrder)
                        .thenComparing(TeachingTimeSlotResponse::getStartTime))
                .toList();
    }

    @Transactional
    public TeachingTimeSlotResponse create(TeachingTimeSlotRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        validateTimeRange(request.getStartTime(), request.getEndTime());
        validateOverlap(centerId, request.getStartTime(), request.getEndTime(), null);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm với ID: " + centerId));

        TeachingTimeSlot timeSlot = TeachingTimeSlot.builder()
                .center(center)
                .name(request.getName().trim())
                .period(request.getPeriod())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(timeSlotRepository.save(timeSlot), centerId);
    }

    @Transactional
    public List<TeachingTimeSlotResponse> quickSetup(QuickSetupRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm với ID: " + centerId));

        List<TeachingTimeSlot> generatedSlots = new ArrayList<>();
        int order = 1;

        // Morning slots
        if (request.getMorningCount() != null && request.getMorningCount() > 0) {
            if (request.getMorningStart() == null) {
                throw new BadRequestException("Vui lòng chọn giờ bắt đầu ca sáng.");
            }
            LocalTime currentStart = request.getMorningStart();
            for (int i = 1; i <= request.getMorningCount(); i++) {
                LocalTime currentEnd = currentStart.plusMinutes(request.getDurationMinutes());
                if (currentEnd.isBefore(currentStart)) {
                    throw new BadRequestException("Giờ kết thúc ca sáng vượt quá khung giờ trong ngày.");
                }
                generatedSlots.add(TeachingTimeSlot.builder()
                        .center(center)
                        .name("Ca sáng " + i)
                        .period(TimeSlotPeriod.MORNING)
                        .startTime(currentStart)
                        .endTime(currentEnd)
                        .displayOrder(order++)
                        .isActive(true)
                        .build());
                currentStart = currentEnd.plusMinutes(request.getGapMinutes());
            }
        }

        // Afternoon slots
        if (request.getAfternoonCount() != null && request.getAfternoonCount() > 0) {
            if (request.getAfternoonStart() == null) {
                throw new BadRequestException("Vui lòng chọn giờ bắt đầu ca chiều.");
            }
            LocalTime currentStart = request.getAfternoonStart();
            for (int i = 1; i <= request.getAfternoonCount(); i++) {
                LocalTime currentEnd = currentStart.plusMinutes(request.getDurationMinutes());
                if (currentEnd.isBefore(currentStart)) {
                    throw new BadRequestException("Giờ kết thúc ca chiều vượt quá khung giờ trong ngày.");
                }
                generatedSlots.add(TeachingTimeSlot.builder()
                        .center(center)
                        .name("Ca chiều " + i)
                        .period(TimeSlotPeriod.AFTERNOON)
                        .startTime(currentStart)
                        .endTime(currentEnd)
                        .displayOrder(order++)
                        .isActive(true)
                        .build());
                currentStart = currentEnd.plusMinutes(request.getGapMinutes());
            }
        }

        // Evening slots
        if (request.getEveningCount() != null && request.getEveningCount() > 0) {
            if (request.getEveningStart() == null) {
                throw new BadRequestException("Vui lòng chọn giờ bắt đầu ca tối.");
            }
            LocalTime currentStart = request.getEveningStart();
            for (int i = 1; i <= request.getEveningCount(); i++) {
                LocalTime currentEnd = currentStart.plusMinutes(request.getDurationMinutes());
                if (currentEnd.isBefore(currentStart)) {
                    throw new BadRequestException("Giờ kết thúc ca tối vượt quá khung giờ trong ngày.");
                }
                generatedSlots.add(TeachingTimeSlot.builder()
                        .center(center)
                        .name("Ca tối " + i)
                        .period(TimeSlotPeriod.EVENING)
                        .startTime(currentStart)
                        .endTime(currentEnd)
                        .displayOrder(order++)
                        .isActive(true)
                        .build());
                currentStart = currentEnd.plusMinutes(request.getGapMinutes());
            }
        }

        if (generatedSlots.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 ca học để thiết lập.");
        }

        // Validate internal overlaps among generated slots
        for (int i = 0; i < generatedSlots.size(); i++) {
            for (int j = i + 1; j < generatedSlots.size(); j++) {
                TeachingTimeSlot s1 = generatedSlots.get(i);
                TeachingTimeSlot s2 = generatedSlots.get(j);
                if (s1.getStartTime().isBefore(s2.getEndTime()) && s1.getEndTime().isAfter(s2.getStartTime())) {
                    throw new BadRequestException(String.format("Các ca học thiết lập bị chồng giồng nhau: %s (%s-%s) và %s (%s-%s)",
                            s1.getName(), s1.getStartTime(), s1.getEndTime(),
                            s2.getName(), s2.getStartTime(), s2.getEndTime()));
                }
            }
        }

        // Validate against existing active slots in DB
        for (TeachingTimeSlot slot : generatedSlots) {
            validateOverlap(centerId, slot.getStartTime(), slot.getEndTime(), null);
        }

        List<TeachingTimeSlot> saved = timeSlotRepository.saveAll(generatedSlots);
        return saved.stream().map(s -> toResponse(s, centerId)).toList();
    }

    @Transactional
    public TeachingTimeSlotResponse update(Long id, TeachingTimeSlotRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        TeachingTimeSlot slot = timeSlotRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca học với ID: " + id));

        boolean isUsed = isTimeSlotUsed(id, centerId);
        boolean timeChanged = !slot.getStartTime().equals(request.getStartTime()) || !slot.getEndTime().equals(request.getEndTime());

        if (isUsed && timeChanged) {
            throw new BusinessRuleException("TIME_SLOT_IN_USE", "Ca học này đang được sử dụng trong lịch học. Để thay đổi khung giờ, hãy tạo một ca mới.");
        }

        if (timeChanged) {
            validateTimeRange(request.getStartTime(), request.getEndTime());
            validateOverlap(centerId, request.getStartTime(), request.getEndTime(), id);
            slot.setStartTime(request.getStartTime());
            slot.setEndTime(request.getEndTime());
        }

        slot.setName(request.getName().trim());
        slot.setPeriod(request.getPeriod());
        if (request.getDisplayOrder() != null) {
            slot.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            slot.setIsActive(request.getIsActive());
        }

        return toResponse(timeSlotRepository.save(slot), centerId);
    }

    @Transactional
    public void deleteOrDeactivate(Long id) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        TeachingTimeSlot slot = timeSlotRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca học với ID: " + id));

        boolean isUsed = isTimeSlotUsed(id, centerId);
        if (isUsed) {
            slot.setIsActive(false);
            timeSlotRepository.save(slot);
        } else {
            timeSlotRepository.delete(slot);
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
    }

    private void validateOverlap(Long centerId, LocalTime startTime, LocalTime endTime, Long excludeId) {
        List<TeachingTimeSlot> overlaps = timeSlotRepository.findOverlappingActiveSlots(centerId, startTime, endTime, excludeId);
        if (!overlaps.isEmpty()) {
            TeachingTimeSlot first = overlaps.get(0);
            throw new DuplicateResourceException(String.format("Khung giờ %s - %s bị trùng với ca học '%s' (%s - %s) đang hoạt động.",
                    startTime, endTime, first.getName(), first.getStartTime(), first.getEndTime()));
        }
    }

    private boolean isTimeSlotUsed(Long timeSlotId, Long centerId) {
        return scheduleRecurringRuleRepository.existsByTimeSlot_IdAndCenter_Id(timeSlotId, centerId);
    }

    private TeachingTimeSlotResponse toResponse(TeachingTimeSlot slot, Long centerId) {
        return TeachingTimeSlotResponse.builder()
                .id(slot.getId())
                .centerId(slot.getCenter().getId())
                .name(slot.getName())
                .period(slot.getPeriod())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .displayOrder(slot.getDisplayOrder())
                .isActive(slot.getIsActive())
                .isUsed(isTimeSlotUsed(slot.getId(), centerId))
                .build();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập.");
        }
        String phoneNumber = authentication.getName();
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với số điện thoại: " + phoneNumber));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm hiện tại.");
        }
    }
}
