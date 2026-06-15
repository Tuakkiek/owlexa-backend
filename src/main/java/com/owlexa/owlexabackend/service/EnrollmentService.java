package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final MembershipRepository membershipRepository;


}
