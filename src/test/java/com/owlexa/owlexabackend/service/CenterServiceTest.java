package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.CenterRequest;
import com.owlexa.owlexabackend.dto.response.CenterResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.access.AccessDeniedException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private CenterService centerService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String phoneNumber) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        phoneNumber,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    @Test
    void create_whenCurrentUserIsOwner_shouldCreateCenterAndMembership() {
        loginAs("0901234567");

        User owner = new User();
        owner.setId(1L);
        owner.setPhoneNumber("0901234567");
        owner.setFullName("Owner A");
        owner.setRole(Role.OWNER);

        CenterRequest request = CenterRequest.builder()
                .name(" Owlexa VSTEP ")
                .subdomain(" Owlexa-HCM ")
                .build();

        when(centerRepository.existsBySubdomain("owlexa-hcm"))
                .thenReturn(false);

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(owner));

        when(centerRepository.save(any(Center.class)))
                .thenAnswer(invocation -> {
                    Center center = invocation.getArgument(0);
                    center.setId(10L);
                    center.setCreatedAt(Instant.now());
                    return center;
                });

        when(membershipRepository.existsByUserIdAndCenterId(1L, 10L))
                .thenReturn(false);

        when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
    void create_whenCurrentUserIsNotOwner_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User student = new User();
        student.setId(1L);
        student.setPhoneNumber("0901234567");
        student.setFullName("Nguyen Van A");
        student.setEmail("student@example.com");
        student.setRole(Role.STUDENT);

        CenterRequest request = CenterRequest.builder()
                .name("Owlexa VSTEP")
                .subdomain("owlexa-hcm")
                .build();

        when(centerRepository.existsBySubdomain("owlexa-hcm"))
                .thenReturn(false);
        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(student));
        assertThatThrownBy(() -> centerService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER can create center");

        verify(centerRepository, never()).save(any(Center.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void create_whenSubdomainAlreadyExists_shouldThrowDuplicateResourceException() {
        CenterRequest request = CenterRequest.builder()
                .name("Owlexa VSTEP")
                .subdomain("owlexa-hcm")
                .build();

        when(centerRepository.existsBySubdomain("owlexa-hcm"))
                .thenReturn(true);
        assertThatThrownBy(() -> centerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Subdomain already exists");

        verify(userRepository, never()).findByPhoneNumber(any());
        verify(centerRepository, never()).save(any(Center.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void findAll_whenCurrentUserIsOwner_shouldReturnOwnerCenters() {
        loginAs("0901234567");

        User owner = new User();
        owner.setId(1L);
        owner.setPhoneNumber("0901234567");
        owner.setFullName("Owner A");
        owner.setRole(Role.OWNER);

        Center center1 = new Center();
        center1.setId(10L);
        center1.setName("Owlexa HCM");
        center1.setSubdomain("owlexa-hcm");
        center1.setOwner(owner);
        center1.setCreatedAt(Instant.now());

        Center center2 = new Center();
        center2.setId(11L);
        center2.setName("Owlexa Hanoi");
        center2.setSubdomain("owlexa-hanoi");
        center2.setOwner(owner);
        center2.setCreatedAt(Instant.now());

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(java.util.Optional.of(owner));

        when(centerRepository.findAllByOwnerId(1L))
                .thenReturn(java.util.List.of(center1, center2));

        java.util.List<CenterResponse> responses = centerService.findAll();

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).getId()).isEqualTo(10L);
        assertThat(responses.get(0).getName()).isEqualTo("Owlexa HCM");
        assertThat(responses.get(0).getSubdomain()).isEqualTo("owlexa-hcm");

        assertThat(responses.get(1).getId()).isEqualTo(11L);
        assertThat(responses.get(1).getName()).isEqualTo("Owlexa Hanoi");
        assertThat(responses.get(1).getSubdomain()).isEqualTo("owlexa-hanoi");
    }

    @Test
    void findById_whenCurrentUserOwnsCenter_shouldReturnCenter() {
        loginAs("0901234567");

        User owner = new User();
        owner.setId(1L);
        owner.setFullName("Nguyen Van A");
        owner.setPhoneNumber("0901234567");
        owner.setRole(Role.OWNER);

        Center center = new Center();
        center.setId(10L);
        center.setOwner(owner);
        center.setName("Owlexa HCM");
        center.setSubdomain("owlexa-hcm");
        center.setCreatedAt(Instant.now());

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(owner));
        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(center));

        CenterResponse response = centerService.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Owlexa HCM");
        assertThat(response.getSubdomain()).isEqualTo("owlexa-hcm");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User currentOwner = new User();
        currentOwner.setId(1L);
        currentOwner.setPhoneNumber("0901234567");
        currentOwner.setFullName("Owner A");
        currentOwner.setRole(Role.OWNER);

        User otherOwner = new User();
        otherOwner.setId(2L);
        otherOwner.setPhoneNumber("0987654321");
        otherOwner.setFullName("Owner B");
        otherOwner.setRole(Role.OWNER);

        Center center = new Center();
        center.setId(10L);
        center.setName("Other Center");
        center.setSubdomain("other-center");
        center.setOwner(otherOwner);
        center.setCreatedAt(Instant.now());

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(currentOwner));

        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(center));

        assertThatThrownBy(() -> centerService.findById(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");
    }

    @Test
    void update_whenCurrentUserOwnsCenter_shouldUpdateCenter() {
        loginAs("0901234567");

        User owner = new User();
        owner.setId(1L);
        owner.setPhoneNumber("0901234567");
        owner.setFullName("Owner A");
        owner.setRole(Role.OWNER);

        Center existingCenter = new Center();
        existingCenter.setId(10L);
        existingCenter.setName("Old Name");
        existingCenter.setSubdomain("old-subdomain");
        existingCenter.setOwner(owner);
        existingCenter.setCreatedAt(Instant.now());

        CenterRequest request = CenterRequest.builder()
                .name(" New Owlexa Name ")
                .subdomain(" New-Subdomain ")
                .build();

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(java.util.Optional.of(owner));

        when(centerRepository.findById(10L))
                .thenReturn(java.util.Optional.of(existingCenter));

        when(centerRepository.findBySubdomain("new-subdomain"))
                .thenReturn(java.util.Optional.empty());

        when(centerRepository.save(any(Center.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
    void update_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {

        loginAs("0901234567");

        User currentOwner = new User();
        currentOwner.setId(1L);
        currentOwner.setPhoneNumber("0901234567");
        currentOwner.setFullName("Owner A");
        currentOwner.setRole(Role.OWNER);

        User otherOwner = new User();
        otherOwner.setId(2L);
        otherOwner.setPhoneNumber("0987654321");
        otherOwner.setFullName("Owner B");
        otherOwner.setRole(Role.OWNER);

        Center existingCenter = new Center();
        existingCenter.setId(10L);
        existingCenter.setName("Other Center");
        existingCenter.setSubdomain("other-center");
        existingCenter.setOwner(otherOwner);
        existingCenter.setCreatedAt(Instant.now());

        CenterRequest request = CenterRequest.builder()
                .name("New Name")
                .subdomain("new-subdomain")
                .build();
        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(currentOwner));

        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(existingCenter));

        assertThatThrownBy(() -> centerService.update(10L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");

        verify(centerRepository, never()).save(any(Center.class));
    }
    @Test
    void delete_whenCurrentUserDoesNotOwnCenter_shouldThrowAccessDeniedException() {
        loginAs("0901234567");

        User currentOwner = new User();
        currentOwner.setId(1L);
        currentOwner.setPhoneNumber("0901234567");
        currentOwner.setFullName("Owner A");
        currentOwner.setRole(Role.OWNER);

        User otherOwner = new User();
        otherOwner.setId(2L);
        otherOwner.setPhoneNumber("0987654321");
        otherOwner.setFullName("Owner B");
        otherOwner.setRole(Role.OWNER);

        Center existingCenter = new Center();
        existingCenter.setId(10L);
        existingCenter.setName("Other Center");
        existingCenter.setSubdomain("other-center");
        existingCenter.setOwner(otherOwner);
        existingCenter.setCreatedAt(Instant.now());

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(currentOwner));

        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(existingCenter));

        assertThatThrownBy(() -> centerService.delete(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not own this center");

        verify(centerRepository, never()).delete(any(Center.class));
    }
//
//    @Test
//    void delete_whenCurrentUserOwnerCenter_shouldDeleteCenter() {
//        loginAs("0901234567");
//
//        User currentOwner = new User();
//        currentOwner.setId(1L);
//        currentOwner.setPhoneNumber("0901234567");
//        currentOwner.setFullName("Owner A");
//        currentOwner.setRole(Role.OWNER);
//
//        Center existingCenter = new Center();
//        existingCenter.setId(10L);
//        existingCenter.setName("Other Center");
//        existingCenter.setSubdomain("other-center");
//        existingCenter.setOwner(currentOwner);
//        existingCenter.setCreatedAt(Instant.now());
//
//        when(userRepository.findByPhoneNumber("0901234567"))
//                .thenReturn(Optional.of(currentOwner));
//
//        when(centerRepository.findById(10L))
//                .thenReturn(Optional.of(existingCenter));
//
//        verify(centerRepository, atLeast(1)).delete(any(Center.class));
//    }
}