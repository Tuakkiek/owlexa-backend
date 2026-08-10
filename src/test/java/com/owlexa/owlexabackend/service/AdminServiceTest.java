package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.admin.AdminStatsResponse;
import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    @Test
    void returnsSystemWideCounts() {
        UserRepository users = mock(UserRepository.class);
        CenterRepository centers = mock(CenterRepository.class);
        when(users.count()).thenReturn(31L);
        when(users.countByRole(RoleName.OWNER)).thenReturn(1L);
        when(users.countByRole(RoleName.TEACHER)).thenReturn(5L);
        when(users.countByRole(RoleName.STUDENT)).thenReturn(20L);
        when(users.countByRole(RoleName.CASHIER)).thenReturn(2L);
        when(users.countByRole(RoleName.ADMIN)).thenReturn(1L);
        when(centers.count()).thenReturn(3L);

        AdminStatsResponse stats = new AdminService(users, centers).getStats();

        assertThat(stats).isEqualTo(new AdminStatsResponse(31, 1, 5, 20, 2, 1, 3));
    }
}
