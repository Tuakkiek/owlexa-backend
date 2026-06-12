package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.StudentRequest;
import com.owlexa.owlexabackend.dto.response.StudentResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.filter.TenantFilter;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantFilter.clearCurrentCenterId();
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

    private void setCurrentCenterId(Long centerId) {
        TenantFilter.setCurrentCenterIdForTest(centerId);
    }

    private User user(Long id, String phoneNumber, Role role) {
        User user = new User();
        user.setId(id);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setFullName("User " + id);
        return user;
    }

    private Center center(
            Long id,
            String name,
            String subdomain,
            User owner
    ) {
        Center center = new Center();
        center.setId(id);
        center.setName(name);
        center.setSubdomain(subdomain);
        center.setOwner(owner);
        center.setCreatedAt(Instant.now());
        return center;
    }

    private StudentRequest request(
            String fullName,
            String email,
            String phoneNumber
    ) {
        return StudentRequest.builder()
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();
    }

    private Membership membership(User user, Center center, User joinedBy) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCenter(center);
        membership.setJoinedByUser(joinedBy);
        return membership;
    }

    @Test
    void create_whenStudentDoesExists_shouldCreateStudentAndMembership() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);

        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        StudentRequest request = request(
                " Nguyen Van A ",
                "student@examle.com",
                "0987654321"
        );

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(owner));

        when(membershipRepository.existsByUserIdAndCenterId(
                1L,
                10L
        ))
                .thenReturn(true);

        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(center));
        when(userRepository.findByPhoneNumber("0987654321"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any()))
                .thenReturn("encoded-temporary-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User student = invocation.getArgument(0);
                    student.setId(100L);
                    return student;
                });

        when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.create(request);

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getCenterId()).isEqualTo(10L);
        assertThat(response.getTemporaryPassword()).isNotBlank();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedStudent = userCaptor.getValue();

        assertThat(savedStudent.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(savedStudent.getEmail()).isEqualTo("student@examle.com");
        assertThat(savedStudent.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(savedStudent.getRole()).isEqualTo(Role.STUDENT);
        assertThat(savedStudent.getPassword()).isEqualTo("encoded-temporary-password");

        ArgumentCaptor<Membership> membershipCaptor =
                ArgumentCaptor.forClass(Membership.class);

        verify(membershipRepository).save(membershipCaptor.capture());

        Membership savedMembership = membershipCaptor.getValue();

        assertThat(savedMembership.getUser().getId()).isEqualTo(100L);
        assertThat(savedMembership.getCenter()).isEqualTo(center);
        assertThat(savedMembership.getJoinedByUser()).isEqualTo(owner);
    }

    @Test
    void create_whenStudentAlreadyExistsAndAlreadyInCenter_shouldReturnStudentWithoutCreatingAgain() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        User existingStudent = user(100L, "0987654321", Role.STUDENT);
        existingStudent.setEmail("student@example.com");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUserIdAndCenterId(1L, 10L)).thenReturn(true);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(existingStudent));
        when(membershipRepository.existsByUserIdAndCenterId(100L, 10L)).thenReturn(true);

        StudentRequest request = request("Nguyen Van A", "student@example.com", "0987654321");

        StudentResponse response = studentService.create(request);

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(response.getCenterId()).isEqualTo(10L);
        assertThat(response.getTemporaryPassword()).isNull();

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }



}
