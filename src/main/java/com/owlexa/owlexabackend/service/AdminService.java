package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.response.AdminStatsResponse;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalOwners(userRepository.countByRole(Role.OWNER))
                .totalTeachers(userRepository.countByRole(Role.TEACHER))
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalCashiers(userRepository.countByRole(Role.CASHIER))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .totalCenters(centerRepository.count())
                .build();
    }
}
