package com.owlexa.owlexabackend.modules.user.service;

import com.owlexa.owlexabackend.modules.user.dto.response.AdminStatsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AdminAuditLogRepository auditLogRepository;

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(
                userRepository, centerRepository, membershipRepository, auditLogRepository);
    }

    @Test
    @DisplayName("getStats: returns aggregated counts")
    void getStats_shouldReturnAggregatedCounts() {
        when(userRepository.count()).thenReturn(150L);
        when(userRepository.countByRole(Role.OWNER)).thenReturn(10L);
        when(userRepository.countByRole(Role.TEACHER)).thenReturn(20L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(100L);
        when(userRepository.countByRole(Role.CASHIER)).thenReturn(15L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(5L);
        when(centerRepository.count()).thenReturn(8L);

        AdminStatsResponse stats = service.getStats();

        assertThat(stats.getTotalUsers()).isEqualTo(150L);
        assertThat(stats.getTotalOwners()).isEqualTo(10L);
        assertThat(stats.getTotalTeachers()).isEqualTo(20L);
        assertThat(stats.getTotalStudents()).isEqualTo(100L);
        assertThat(stats.getTotalCashiers()).isEqualTo(15L);
        assertThat(stats.getTotalAdmins()).isEqualTo(5L);
        assertThat(stats.getTotalCenters()).isEqualTo(8L);
    }

    @Test
    @DisplayName("getStats: empty system (zero counts)")
    void getStats_whenEmptySystem_shouldReturnZeroCounts() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(any())).thenReturn(0L);
        when(centerRepository.count()).thenReturn(0L);

        AdminStatsResponse stats = service.getStats();

        assertThat(stats.getTotalUsers()).isZero();
        assertThat(stats.getTotalCenters()).isZero();
    }
}
