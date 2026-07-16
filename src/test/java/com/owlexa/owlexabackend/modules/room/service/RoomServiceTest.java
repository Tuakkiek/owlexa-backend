package com.owlexa.owlexabackend.modules.room.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private RoomService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long ROOM_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new RoomService(roomRepository, centerRepository, userRepository, membershipRepository);
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
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Center buildCenter(Long id) {
        Center center = new Center();
        center.setId(id);
        return center;
    }

    private Room buildRoom(Long id, Long centerId, String code, String name) {
        Center center = new Center();
        center.setId(centerId);
        Room room = new Room();
        room.setId(id);
        room.setCode(code);
        room.setName(name);
        room.setCenter(center);
        room.setIsActive(true);
        return room;
    }

    private RoomRequest buildCreateRequest() {
        return RoomRequest.builder()
                .code("P201")
                .name("Phòng 201")
                .capacity(30)
                .build();
    }

    @Test
    @DisplayName("create: valid request → creates room")
    void create_whenValid_shouldCreateRoom() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(roomRepository.existsByCodeAndCenter_Id("P201", CENTER_ID)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setId(ROOM_ID);
            return r;
        });

        RoomResponse response = service.create(buildCreateRequest());

        assertThat(response.getId()).isEqualTo(ROOM_ID);
        assertThat(response.getCode()).isEqualTo("P201");
        assertThat(response.getName()).isEqualTo("Phòng 201");
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
    }

    @Test
    @DisplayName("create: duplicate code in center → DuplicateResourceException")
    void create_whenDuplicateCode_shouldThrowDuplicate() {
        when(roomRepository.existsByCodeAndCenter_Id("P201", CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create: not OWNER → AccessDeniedException")
    void create_whenNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("0900000002", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("0900000002")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findAll: returns active rooms in center")
    void findAll_shouldReturnActiveRooms() {
        when(roomRepository.findAllByCenter_IdAndIsActiveTrue(CENTER_ID))
                .thenReturn(List.of(buildRoom(1L, CENTER_ID, "P201", "Phòng 201"),
                        buildRoom(2L, CENTER_ID, "P202", "Phòng 202")));

        List<RoomResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getCenterId()).isEqualTo(CENTER_ID);
    }

    @Test
    @DisplayName("findById: room exists in center → returns room")
    void findById_whenExists_shouldReturnRoom() {
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID))
                .thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID, "P201", "Phòng 201")));

        RoomResponse response = service.findById(ROOM_ID);

        assertThat(response.getId()).isEqualTo(ROOM_ID);
        assertThat(response.getCode()).isEqualTo("P201");
    }

    @Test
    @DisplayName("update: valid → updates room")
    void update_whenValid_shouldUpdateRoom() {
        Room existing = buildRoom(ROOM_ID, CENTER_ID, "P201", "Phòng 201");
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomRequest req = RoomRequest.builder()
                .code("P201")
                .name("Phòng 201 Updated")
                .capacity(35)
                .build();

        RoomResponse response = service.update(ROOM_ID, req);

        assertThat(response.getName()).isEqualTo("Phòng 201 Updated");
        assertThat(response.getCapacity()).isEqualTo(35);
    }

    @Test
    @DisplayName("delete: room exists → deletes")
    void delete_whenExists_shouldDelete() {
        when(roomRepository.findByIdAndCenter_Id(ROOM_ID, CENTER_ID))
                .thenReturn(Optional.of(buildRoom(ROOM_ID, CENTER_ID, "P201", "Phòng 201")));

        service.delete(ROOM_ID);
    }

    @Test
    @DisplayName("delete: room not found → ResourceNotFoundException")
    void delete_whenNotFound_shouldThrowResourceNotFound() {
        when(roomRepository.findByIdAndCenter_Id(999L, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
