package com.owlexa.owlexabackend.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.service.CenterService;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private FeeRecordRepository feeRecordRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private CenterService centerService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String phoneNumber) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(phoneNumber, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(Long id, String phoneNumber, Role role) {
        User user = new User();
        user.setId(id);
        user.setPhoneNumber(phoneNumber);
        user.setFullName("User " + id);
        user.setRole(role);
        return user;
    }

    private Center center(Long id, String name, String subdomain, User owner) {
        Center center = new Center();
        center.setId(id);
        center.setName(name);
        center.setSubdomain(subdomain);
        center.setOwner(owner);
        center.setCreatedAt(Instant.now());
        return center;
    }

    private CenterRequest request(String name, String subdomain) {
        return CenterRequest.builder()
                .name(name)
                .subdomain(subdomain)
                .build();
    }

    @Test
    void create_whenCurrentUserIsOwner_shouldCreateCenterAndMembership() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        CenterRequest request = request(" Owlexa VSTEP ", " Owlexa-HCM ");

        when(centerRepository.existsBySubdomain("owlexa-hcm")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findAllByOwner_Id(1L)).thenReturn(List.of());
        when(centerRepository.save(any(Center.class))).thenAnswer(invocation -> {
            Center savedCenter = invocation.getArgument(0);
            savedCenter.setId(10L);
            savedCenter.setCreatedAt(Instant.now());
            return savedCenter;
        });
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(false);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CenterResponse response = centerService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Owlexa VSTEP");
        assertThat(response.getSubdomain()).isEqualTo("owlexa-hcm");
        assertThat(response.getCreatedAt()).isNotNull();

        ArgumentCaptor<Center> centerCaptor = ArgumentCaptor.forClass(Center.class);
        verify(centerRepository).save(centerCaptor.capture());

        Center savedCenter = centerCaptor.getValue();
        assertThat(savedCenter.getName()).isEqualTo("Owlexa VSTEP");
        assertThat(savedCenter.getSubdomain()).isEqualTo("owlexa-hcm");
        assertThat(savedCenter.getOwner()).isEqualTo(owner);

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membershipCaptor.capture());

        Membership savedMembership = membershipCaptor.getValue();
        assertThat(savedMembership.getUser()).isEqualTo(owner);
        assertThat(savedMembership.getCenter().getId()).isEqualTo(10L);
        assertThat(savedMembership.getJoinedByUser()).isEqualTo(owner);
        assertThat(savedMembership.getJoinedAt()).isNotNull();
    }

    @Test
    void create_whenMembershipAlreadyExists_shouldNotCreateMembershipAgain() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        CenterRequest request = request("Owlexa VSTEP", "owlexa-hcm");

        when(centerRepository.existsBySubdomain("owlexa-hcm")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findAllByOwner_Id(1L)).thenReturn(List.of());
        when(centerRepository.save(any(Center.class))).thenAnswer(invocation -> {
            Center savedCenter = invocation.getArgument(0);
            savedCenter.setId(10L);
            savedCenter.setCreatedAt(Instant.now());
            return savedCenter;
        });
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(true);

        CenterResponse response = centerService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void create_whenSubdomainAlreadyExists_shouldThrowDuplicateResourceException() {
        CenterRequest request = request("Owlexa VSTEP", " Owlexa-HCM ");

        when(centerRepository.existsBySubdomain("owlexa-hcm")).thenReturn(true);

        assertThatThrownBy(() -> centerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Subdomain already exists");

        verify(userRepository, never()).findByPhoneNumber(any());
        verify(centerRepository, never()).save(any(Center.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void create_whenCurrentUserIsNotOwner_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User student = user(1L, "0901234567", Role.STUDENT);
        CenterRequest request = request("Owlexa VSTEP", "owlexa-hcm");

        when(centerRepository.existsBySubdomain("owlexa-hcm")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> centerService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER can create center");

        verify(centerRepository, never()).save(any(Center.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void findAll_whenCurrentUserIsOwner_shouldReturnOwnerCenters() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center center1 = center(10L, "Owlexa HCM", "owlexa-hcm", owner);
        Center center2 = center(11L, "Owlexa Hanoi", "owlexa-hanoi", owner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findAllByOwner_Id(1L)).thenReturn(List.of(center1, center2));

        List<CenterResponse> responses = centerService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(10L);
        assertThat(responses.get(0).getName()).isEqualTo("Owlexa HCM");
        assertThat(responses.get(0).getSubdomain()).isEqualTo("owlexa-hcm");
        assertThat(responses.get(1).getId()).isEqualTo(11L);
        assertThat(responses.get(1).getName()).isEqualTo("Owlexa Hanoi");
        assertThat(responses.get(1).getSubdomain()).isEqualTo("owlexa-hanoi");
    }

    @Test
    void findAll_whenCurrentUserIsNotOwner_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User student = user(1L, "0901234567", Role.STUDENT);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> centerService.findAll())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER can view centers");

        verify(centerRepository, never()).findAllByOwner_Id(any());
    }

    @Test
    void findById_whenCurrentUserOwnsCenter_shouldReturnCenter() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));

        CenterResponse response = centerService.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Owlexa HCM");
        assertThat(response.getSubdomain()).isEqualTo("owlexa-hcm");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_whenCenterNotFound_shouldThrowResourceNotFoundException() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> centerService.findById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Center not found with id: 10");
    }

    @Test
    void findById_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User currentOwner = user(1L, "0901234567", Role.OWNER);
        User otherOwner = user(2L, "0987654321", Role.OWNER);
        Center otherCenter = center(10L, "Other Center", "other-center", otherOwner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(currentOwner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(otherCenter));

        assertThatThrownBy(() -> centerService.findById(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");
    }

    @Test
    void update_whenCurrentUserOwnsCenter_shouldUpdateCenter() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center existingCenter = center(10L, "Old Name", "old-subdomain", owner);
        CenterRequest request = request(" New Owlexa Name ", " New-Subdomain ");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));
        when(centerRepository.findBySubdomain("new-subdomain")).thenReturn(Optional.empty());
        when(centerRepository.save(any(Center.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CenterResponse response = centerService.update(10L, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("New Owlexa Name");
        assertThat(response.getSubdomain()).isEqualTo("new-subdomain");

        ArgumentCaptor<Center> centerCaptor = ArgumentCaptor.forClass(Center.class);
        verify(centerRepository).save(centerCaptor.capture());

        Center savedCenter = centerCaptor.getValue();
        assertThat(savedCenter.getName()).isEqualTo("New Owlexa Name");
        assertThat(savedCenter.getSubdomain()).isEqualTo("new-subdomain");
        assertThat(savedCenter.getOwner()).isEqualTo(owner);
    }

    @Test
    void update_whenSubdomainBelongsToAnotherCenter_shouldThrowDuplicateResourceException() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center existingCenter = center(10L, "Old Name", "old-subdomain", owner);
        Center anotherCenter = center(99L, "Another Center", "new-subdomain", owner);
        CenterRequest request = request("New Name", "new-subdomain");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));
        when(centerRepository.findBySubdomain("new-subdomain")).thenReturn(Optional.of(anotherCenter));

        assertThatThrownBy(() -> centerService.update(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Subdomain already exists");

        verify(centerRepository, never()).save(any(Center.class));
    }

    @Test
    void update_whenSubdomainBelongsToSameCenter_shouldAllowUpdate() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center existingCenter = center(10L, "Old Name", "old-subdomain", owner);
        CenterRequest request = request("New Name", "old-subdomain");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));
        when(centerRepository.findBySubdomain("old-subdomain")).thenReturn(Optional.of(existingCenter));
        when(centerRepository.save(any(Center.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CenterResponse response = centerService.update(10L, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getSubdomain()).isEqualTo("old-subdomain");
    }

    @Test
    void update_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User currentOwner = user(1L, "0901234567", Role.OWNER);
        User otherOwner = user(2L, "0987654321", Role.OWNER);
        Center existingCenter = center(10L, "Other Center", "other-center", otherOwner);
        CenterRequest request = request("New Name", "new-subdomain");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(currentOwner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));

        assertThatThrownBy(() -> centerService.update(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");

        verify(centerRepository, never()).save(any(Center.class));
    }

    @Test
    void delete_whenCurrentUserOwnsCenter_shouldThrowBadRequestException() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);
        Center existingCenter = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));

        assertThatThrownBy(() -> centerService.delete(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể xóa trung tâm duy nhất của chủ sở hữu.");

        verify(centerRepository, never()).delete(any(Center.class));
    }

    @Test
    void delete_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User currentOwner = user(1L, "0901234567", Role.OWNER);
        User otherOwner = user(2L, "0987654321", Role.OWNER);
        Center existingCenter = center(10L, "Other Center", "other-center", otherOwner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(currentOwner));
        when(centerRepository.findById(10L)).thenReturn(Optional.of(existingCenter));

        assertThatThrownBy(() -> centerService.delete(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");

        verify(centerRepository, never()).delete(any(Center.class));
    }

    @Test
    void delete_whenCenterNotFound_shouldThrowResourceNotFoundException() {
        loginAs("0901234567");

        User owner = user(1L, "0901234567", Role.OWNER);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> centerService.delete(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Center not found with id: 10");

        verify(centerRepository, never()).delete(any(Center.class));
    }
}