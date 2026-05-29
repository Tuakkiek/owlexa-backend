package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.BulkStudentRequest;
import com.owlexa.owlexabackend.dto.request.StudentRequest;
import com.owlexa.owlexabackend.dto.response.BulkStudentError;
import com.owlexa.owlexabackend.dto.response.BulkStudentResult;
import com.owlexa.owlexabackend.dto.response.StudentResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.BulkStudentValidationException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;

    // Create
    @Transactional
    public StudentResponse create(StudentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        Center center = centerRepository
                .findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        assertOwnerAndCenterMembership(currentUser, centerId);


        User studentUser;
        String temporaryPassword = null;

        // Check if student is present
        var existingStudent = userRepository.findByPhoneNumber(request.getPhoneNumber());

        if (existingStudent.isPresent()) {
            studentUser = existingStudent.get();

            if (studentUser.getRole() != Role.STUDENT) {
                throw new BadRequestException("User is not a STUDENT");
            }
            boolean existsMembership = membershipRepository.existsByUserIdAndCenterId(studentUser.getId(), centerId);

            if (!existsMembership) {
                createMembership(studentUser, center, currentUser);
            }
        }  else {
            temporaryPassword = generateTemporaryPassword();
            studentUser = new User();
            studentUser.setPhoneNumber(request.getPhoneNumber());
            studentUser.setEmail(normalizeOptionalEmail(request.getEmail()));
            studentUser.setFullName(request.getFullName());
            studentUser.setRole(Role.STUDENT);
            studentUser.setPassword(passwordEncoder.encode(temporaryPassword));
            studentUser = userRepository.save(studentUser);
            createMembership(studentUser, center, currentUser);
        }
        return toResponse(studentUser, centerId, temporaryPassword);
    }

    // Update
    @Transactional
    public StudentResponse update(Long studentId,StudentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(studentId, centerId, Role.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this center"));

        User student = membership.getUser();

        String phoneNumber = request.getPhoneNumber();
        String email = normalizeOptionalEmail(request.getEmail());
        String fullName = request.getFullName().trim();

        if (student.getPhoneNumber().equals(phoneNumber) || student.getEmail().equals(email) || student.getFullName().equals(fullName)) {
            throw new BadRequestException("The updated information is a duplicate of the current information");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Phone number is already exists");
        }
        if (email != null && !email.equals(student.getEmail()) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        student.setPhoneNumber(phoneNumber);
        student.setEmail(email);
        student.setFullName(fullName);

        student = userRepository.save(student);

        return toResponse(student, centerId, null);
    }

    // Bulk Crate
    @Transactional
    public List<BulkStudentResult> bulkCreate(@NonNull BulkStudentRequest request) {
        if (request.getStudents() == null || request.getStudents().isEmpty()) {
            throw new BadRequestException("Students list must not be empty");
        }

        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        assertOwnerAndCenterMembership(currentUser, centerId);

        List<BulkStudentError> errors = new ArrayList<>();

        Set<String> seenPhoneNumbers = new HashSet<>();

        // PHASE 1: VALIDATION ONLY
        for (int i = 0; i < request.getStudents().size(); i++) {
            BulkStudentRequest.Item item = request.getStudents().get(i);
            int row = i + 1;

            if (item.getPhoneNumber() == null || item.getPhoneNumber().isBlank()
                    || item.getFullName() == null || item.getFullName().isBlank()) {

                BulkStudentError error = new BulkStudentError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus("INVALID_INPUT");
                error.setMessage("Phone number and full name are required");

                errors.add(error);
                continue;
            }

            String phoneNumber = item.getPhoneNumber().trim();

            if (!seenPhoneNumbers.add(phoneNumber)) {
                BulkStudentError error = new BulkStudentError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus("INVALID_INPUT");
                error.setMessage("Duplicate phone number in request");

                errors.add(error);
                continue;
            }

            if (item.getEmail() != null && !item.getEmail().isBlank()
                    && userRepository.existsByEmail(item.getEmail())) {

                BulkStudentError error = new BulkStudentError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus("INVALID_INPUT");
                error.setMessage("Email already exists");

                errors.add(error);
                continue;
            }

            var existingUser = userRepository.findByPhoneNumber(phoneNumber);
            if (existingUser.isPresent() && existingUser.get().getRole() != Role.STUDENT) {

                BulkStudentError error = new BulkStudentError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus("ROLE_CONFLICT");
                error.setMessage("Existing user role is not STUDENT");

                errors.add(error);
            }
        }


        if (!errors.isEmpty()) {
            throw new BulkStudentValidationException(errors);
        }

        // PHASE 2: EXECUTION
        List<BulkStudentResult> results = new ArrayList<>();
        for (BulkStudentRequest.Item item : request.getStudents()) {

            String phoneNumber = item.getPhoneNumber().trim();
            String fullName = item.getFullName().trim();
            String email = normalizeOptionalEmail(item.getEmail());

            var existingUser = userRepository.findByPhoneNumber(phoneNumber);

            if (existingUser.isPresent()) {
                User studentUser = existingUser.get();

                boolean existsMembership =
                        membershipRepository.existsByUserIdAndCenterId(studentUser.getId(), centerId);

                BulkStudentResult result = new BulkStudentResult();
                result.setPhoneNumber(phoneNumber);
                result.setTemporaryPassword(null);

                if (existsMembership) {
                    result.setStatus("ALREADY_IN_CENTER");
                } else {
                    createMembership(studentUser, center, currentUser);
                    result.setStatus("ADDED_TO_CENTER");
                }

                results.add(result);
                continue;
            }

            String temporaryPassword = generateTemporaryPassword();

            User studentUser = new User();
            studentUser.setPhoneNumber(phoneNumber);
            studentUser.setEmail(email);
            studentUser.setFullName(fullName);
            studentUser.setRole(Role.STUDENT);
            studentUser.setPassword(passwordEncoder.encode(temporaryPassword));

            studentUser = userRepository.save(studentUser);

            createMembership(studentUser, center, currentUser);

            BulkStudentResult result = new BulkStudentResult();
            result.setPhoneNumber(phoneNumber);
            result.setStatus("CREATED");
            result.setTemporaryPassword(temporaryPassword);

            results.add(result);
        }

        return results;
    }

    // Find all
    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        return membershipRepository.findAllByCenterIdAndUserRole(centerId, Role.STUDENT)
                .stream()
                .map(Membership::getUser)
                .map(user -> toResponse(user, centerId, null))
                .toList();
    }

    // Delete
    @Transactional
    public void delete(Long studentId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(currentUser.getId(), centerId, Role.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this center"));

        membershipRepository.delete(membership);
    }

    // HELPER
    private void createMembership(User studentUser, Center center, User joinedByUser) {
        Membership membership = new Membership();
        membership.setUser(studentUser);
        membership.setCenter(center);
        membership.setJoinedByUser(joinedByUser);
        membershipRepository.save(membership);
    }

    private String generateTemporaryPassword() {
        int length = 8 + SECURE_RANDOM.nextInt(3);
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can add student to center");
        }

        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private StudentResponse toResponse(User user, Long centerId, String temporaryPassword) {
        return StudentResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .centerId(centerId)
                .temporaryPassword(temporaryPassword)
                .build();
    }
}
