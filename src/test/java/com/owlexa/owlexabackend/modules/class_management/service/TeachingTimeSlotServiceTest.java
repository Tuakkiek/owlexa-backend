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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeachingTimeSlotServiceTest {

    @Mock private TeachingTimeSlotRepository timeSlotRepository;
    @Mock private ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    @Mock private UserRepository userRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;

    @InjectMocks
    private TeachingTimeSlotService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(CENTER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);

        lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
        lenient().when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(new Center()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CASE 1: Create valid TeachingTimeSlot -> success")
    void create_whenValid_shouldCreateTimeSlot() {
        TeachingTimeSlotRequest req = TeachingTimeSlotRequest.builder()
                .name("Ca sáng 1")
                .period(TimeSlotPeriod.MORNING)
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(8, 30))
                .displayOrder(1)
                .isActive(true)
                .build();

        when(timeSlotRepository.findOverlappingActiveSlots(eq(CENTER_ID), eq(LocalTime.of(7, 0)), eq(LocalTime.of(8, 30)), any()))
                .thenReturn(List.of());
        when(timeSlotRepository.save(any(TeachingTimeSlot.class))).thenAnswer(invocation -> {
            TeachingTimeSlot slot = invocation.getArgument(0);
            slot.setId(100L);
            return slot;
        });

        TeachingTimeSlotResponse response = service.create(req);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("Ca sáng 1");
        assertThat(response.getPeriod()).isEqualTo(TimeSlotPeriod.MORNING);
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    @DisplayName("CASE 2: startTime >= endTime -> reject")
    void create_whenStartTimeNotBeforeEndTime_shouldThrowBadRequest() {
        TeachingTimeSlotRequest req = TeachingTimeSlotRequest.builder()
                .name("Ca lỗi")
                .period(TimeSlotPeriod.MORNING)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(8, 0))
                .build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Giờ bắt đầu phải trước giờ kết thúc");
    }

    @Test
    @DisplayName("CASE 3: Overlapping active slot -> reject")
    void create_whenOverlappingActiveSlot_shouldThrowDuplicateResource() {
        TeachingTimeSlot existing = TeachingTimeSlot.builder()
                .id(1L)
                .name("Ca 1")
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(8, 30))
                .build();

        when(timeSlotRepository.findOverlappingActiveSlots(eq(CENTER_ID), eq(LocalTime.of(8, 0)), eq(LocalTime.of(9, 30)), any()))
                .thenReturn(List.of(existing));

        TeachingTimeSlotRequest req = TeachingTimeSlotRequest.builder()
                .name("Ca 2 trùng")
                .period(TimeSlotPeriod.MORNING)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("bị trùng với ca học");
    }

    @Test
    @DisplayName("CASE 4: Boundary slot (gap = 0, startTime == endTime of prev slot) -> allowed")
    void create_whenBoundarySlot_shouldAllow() {
        TeachingTimeSlotRequest req = TeachingTimeSlotRequest.builder()
                .name("Ca 2 kề")
                .period(TimeSlotPeriod.MORNING)
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(timeSlotRepository.findOverlappingActiveSlots(eq(CENTER_ID), eq(LocalTime.of(8, 30)), eq(LocalTime.of(10, 0)), any()))
                .thenReturn(List.of());
        when(timeSlotRepository.save(any(TeachingTimeSlot.class))).thenAnswer(i -> {
            TeachingTimeSlot s = i.getArgument(0);
            s.setId(101L);
            return s;
        });

        TeachingTimeSlotResponse response = service.create(req);
        assertThat(response.getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("CASE 7: Delete time slot currently referenced -> deactivate instead of hard delete")
    void delete_whenReferenced_shouldDeactivate() {
        TeachingTimeSlot slot = TeachingTimeSlot.builder()
                .id(50L)
                .name("Ca đang dùng")
                .isActive(true)
                .build();

        when(timeSlotRepository.findByIdAndCenter_Id(50L, CENTER_ID)).thenReturn(Optional.of(slot));
        when(scheduleRecurringRuleRepository.existsByTimeSlot_IdAndCenter_Id(50L, CENTER_ID)).thenReturn(true);

        service.deleteOrDeactivate(50L);

        assertThat(slot.getIsActive()).isFalse();
        verify(timeSlotRepository).save(slot);
        verify(timeSlotRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CASE 8-10: Quick setup duration 90, gap 5 -> correct time slots generated")
    void quickSetup_shouldGenerateCorrectTimeSlots() {
        QuickSetupRequest req = QuickSetupRequest.builder()
                .durationMinutes(90)
                .gapMinutes(5)
                .morningStart(LocalTime.of(7, 0))
                .morningCount(2)
                .afternoonStart(LocalTime.of(13, 0))
                .afternoonCount(2)
                .eveningStart(LocalTime.of(18, 15))
                .eveningCount(2)
                .build();

        when(timeSlotRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<TeachingTimeSlotResponse> result = service.quickSetup(req);

        assertThat(result).hasSize(6);

        // Morning
        assertThat(result.get(0).getName()).isEqualTo("Ca sáng 1");
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(8, 30));

        assertThat(result.get(1).getName()).isEqualTo("Ca sáng 2");
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalTime.of(8, 35));
        assertThat(result.get(1).getEndTime()).isEqualTo(LocalTime.of(10, 5));

        // Afternoon
        assertThat(result.get(2).getName()).isEqualTo("Ca chiều 1");
        assertThat(result.get(2).getStartTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(result.get(2).getEndTime()).isEqualTo(LocalTime.of(14, 30));

        assertThat(result.get(3).getName()).isEqualTo("Ca chiều 2");
        assertThat(result.get(3).getStartTime()).isEqualTo(LocalTime.of(14, 35));
        assertThat(result.get(3).getEndTime()).isEqualTo(LocalTime.of(16, 5));

        // Evening
        assertThat(result.get(4).getName()).isEqualTo("Ca tối 1");
        assertThat(result.get(4).getStartTime()).isEqualTo(LocalTime.of(18, 15));
        assertThat(result.get(4).getEndTime()).isEqualTo(LocalTime.of(19, 45));

        assertThat(result.get(5).getName()).isEqualTo("Ca tối 2");
        assertThat(result.get(5).getStartTime()).isEqualTo(LocalTime.of(19, 50));
        assertThat(result.get(5).getEndTime()).isEqualTo(LocalTime.of(21, 20));
    }
}
