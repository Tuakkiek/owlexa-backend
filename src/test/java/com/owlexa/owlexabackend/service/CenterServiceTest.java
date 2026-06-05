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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
                .thenReturn(java.util.Optional.of(owner));

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
}