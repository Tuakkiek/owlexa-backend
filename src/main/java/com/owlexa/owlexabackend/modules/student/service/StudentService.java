package com.owlexa.owlexabackend.modules.student.service;
import com.owlexa.owlexabackend.modules.student.dto.request.BulkStudentRequest;
import com.owlexa.owlexabackend.modules.student.dto.request.StudentRequest;
import com.owlexa.owlexabackend.modules.student.dto.response.BulkStudentError;
import com.owlexa.owlexabackend.modules.student.dto.response.BulkStudentResult;
import com.owlexa.owlexabackend.modules.student.dto.response.StudentResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.service.UserPermissionService;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BulkStudentValidationException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*!?";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPermissionService userPermissionService;

    // Create
    @Transactional
    public StudentResponse create(StudentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Center center = centerRepository
                .findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        String phoneNumber = request.getPhoneNumber().trim();
        String email = normalizeOptionalEmail(request.getEmail());
        String fullName = request.getFullName().trim();

        String temporaryPassword = null;

        Optional<User> existingStudent = userRepository.findByPhoneNumber(phoneNumber);

        User studentUser;

        if (existingStudent.isPresent()) {
            studentUser = existingStudent.get();

            if (studentUser.getRole() != Role.STUDENT) {
                throw new BadRequestException("User is not a STUDENT");
            }
            boolean membership = membershipRepository.existsByUser_IdAndCenter_Id(studentUser.getId(), centerId);

            if (!membership) {
                createMembership(studentUser, center, currentUser);
            }
        } else {

            temporaryPassword = generateTemporaryPassword();

            studentUser = createStudent(
                    phoneNumber,
                    email,
                    fullName,
                    temporaryPassword
            );

            createMembership(studentUser, center, currentUser);
        }

        applyPermissionOverridesIfPresent(studentUser.getId(), request.getPermissionOverrides());

        return toResponse(studentUser, centerId, temporaryPassword);

    }

    // Update
    @Transactional
    public StudentResponse update(Long studentId,StudentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(studentId, centerId, Role.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this center"));

        User student = membership.getUser();

        String phoneNumber = request.getPhoneNumber().trim();
        String email = normalizeOptionalEmail(request.getEmail());
        String fullName = request.getFullName().trim();

        if (!student.getPhoneNumber().equals(phoneNumber)
            && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        if (email != null
            && !email.equals(student.getEmail())
            && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        student.setPhoneNumber(phoneNumber);
        student.setEmail(email);
        student.setFullName(fullName);

        User savedStudent = userRepository.save(student);

        applyPermissionOverridesIfPresent(savedStudent.getId(), request.getPermissionOverrides());

        return toResponse(savedStudent, centerId, null);
    }

    // Bulk Crate
    @Transactional
    public List<BulkStudentResult> bulkCreate(@NonNull BulkStudentRequest request) {

        if (request.getStudents() == null || request.getStudents().isEmpty()) {
            throw new BadRequestException("Students list must not be empty");
        }

        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));


        List<BulkStudentError> errors = new ArrayList<>();

        Set<String> seenPhoneNumbers = new HashSet<>();

        // PHASE 1: VALIDATION ONLY
        for (int i = 0; i < request.getStudents().size(); i++) {
            BulkStudentRequest.Item item = request.getStudents().get(i);
            int row = i + 1;

            if (item.getPhoneNumber() == null || item.getPhoneNumber().isBlank()
                || item.getFullName() == null || item.getFullName().isBlank()) {
                errors.add(buildBulkError(
                        row,
                        item.getPhoneNumber(),
                        "INVALID_INPUT",
                        "Phone number and full name are required"
                ));
                continue;
            }

            String phoneNumber = item.getPhoneNumber().trim();
            String email = normalizeOptionalEmail(item.getEmail());

            if (!seenPhoneNumbers.add(phoneNumber)) {
                errors.add(buildBulkError(
                        row,
                        phoneNumber,
                        "INVALID_INPUT",
                        "Duplicate phone number in request"
                ));
                continue;
            }

            if (email != null && userRepository.existsByEmail(email)) {
                errors.add(buildBulkError(
                        row,
                        phoneNumber,
                        "INVALID_INPUT",
                        "Email already exists"
                ));
                continue;
            }

            var existingUser = userRepository.findByPhoneNumber(phoneNumber);
            if (existingUser.isPresent() && existingUser.get().getRole() != Role.STUDENT) {
                errors.add(buildBulkError(
                        row,
                        phoneNumber,
                        "ROLE_CONFLICT",
                        "Exists user role is not STUDENT"
                ));
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
                        membershipRepository.existsByUser_IdAndCenter_Id(studentUser.getId(), centerId);

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

        return membershipRepository.findAllByCenter_IdAndUserRole(centerId, Role.STUDENT)
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
                .findByUser_IdAndCenter_IdAndUserRole(studentId, centerId, Role.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this center"));

        membershipRepository.delete(membership);
    }

    // HELPER
    // Create student
    private User createStudent(String phoneNumber, String email, String fullName, String password) {
        User studentUser = new User();

        studentUser.setPhoneNumber(phoneNumber);
        studentUser.setEmail(email);
        studentUser.setFullName(fullName);
        studentUser.setRole(Role.STUDENT);
        studentUser.setPassword(passwordEncoder.encode(password));

        return userRepository.save(studentUser);
    }
    // Create membership
    private void createMembership(User studentUser, Center center, User joinedByUser) {
        Membership membership = new Membership();
        membership.setUser(studentUser);
        membership.setCenter(center);
        membership.setJoinedByUser(joinedByUser);
        membershipRepository.save(membership);
    }

    private void applyPermissionOverridesIfPresent(Long userId,
                                                    java.util.List<com.owlexa.owlexabackend.modules.user.dto.request.PermissionOverrideItem> overrides) {
        if (overrides != null && !overrides.isEmpty()) {
            userPermissionService.applyOverrides(userId,
                    BulkPermissionOverrideRequest.builder().overrides(overrides).build());
        }
    }

    // Build bulk error
    private BulkStudentError buildBulkError(
            int row,
            String phoneNumber,
            String status,
            String message
    ) {
        BulkStudentError error = new BulkStudentError();
        error.setRow(row);
        error.setPhoneNumber(phoneNumber);
        error.setStatus(status);
        error.setMessage(message);

        return error;
    }

    // Generate temporary password
    private String generateTemporaryPassword() {
        int length = 8 + SECURE_RANDOM.nextInt(3);
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    // Assert owner and center membership
    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can add student to center");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    // Get current user
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName()) ) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    // Required current centerId
    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    // Normalize optional email
    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    // To response
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
