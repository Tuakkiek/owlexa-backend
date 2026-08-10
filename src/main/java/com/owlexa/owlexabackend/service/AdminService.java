package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.admin.AdminStatsResponse;
import com.owlexa.owlexabackend.entity.RoleName;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;

    public AdminService(UserRepository userRepository, CenterRepository centerRepository) {
        this.userRepository = userRepository;
        this.centerRepository = centerRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(RoleName.OWNER),
                userRepository.countByRole(RoleName.TEACHER),
                userRepository.countByRole(RoleName.STUDENT),
                userRepository.countByRole(RoleName.CASHIER),
                userRepository.countByRole(RoleName.ADMIN),
                centerRepository.count()
        );
    }
}
