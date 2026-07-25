package com.owlexa.owlexabackend.modules.teacher.service;
import com.owlexa.owlexabackend.modules.teacher.entity.TeacherCenterProfile;
import com.owlexa.owlexabackend.modules.teacher.repository.TeacherCenterProfileRepository;
import com.owlexa.owlexabackend.modules.teacher.dto.request.BulkTeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.request.TeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.response.BulkTeacherError;
import com.owlexa.owlexabackend.modules.teacher.dto.response.BulkTeacherResult;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.attendance.entity.AttendanceStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.DeviceTypeConverter;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.attendance.entity.Attendance;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.teacher.entity.BulkTeacherStatus;
import com.owlexa.owlexabackend.modules.user.entity.UserPermission;
import com.owlexa.owlexabackend.modules.document.entity.DocumentType;
import com.owlexa.owlexabackend.modules.payment.entity.PaymentMethod;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.DeviceType;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.user.dto.request.BulkPermissionOverrideRequest;
import com.owlexa.owlexabackend.modules.user.entity.Permission;
import com.owlexa.owlexabackend.modules.user.service.UserPermissionService;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BulkTeacherValidationException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherService.class);

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*!?";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherCenterProfileRepository teacherCenterProfileRepository;
    private final UserPermissionService userPermissionService;

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        LOGGER.info("=== TeacherService.create() START ===");
        LOGGER.info("  request.fullName    = '{}'", request.getFullName());
        LOGGER.info("  request.phoneNumber = '{}'", request.getPhoneNumber());
        LOGGER.info("  request.email       = '{}'", request.getEmail());

        LOGGER.info("[STEP 1] getCurrentUser()...");
        User currentUser = getCurrentUser();
        LOGGER.info("  currentUser.id={}, phone={}, role={}", currentUser.getId(), currentUser.getPhoneNumber(), currentUser.getRole());

        LOGGER.info("[STEP 2] requiredCurrentCenterId()...");
        Long centerId = requiredCurrentCenterId();
        LOGGER.info("  centerId = {}", centerId);

        LOGGER.info("[STEP 3] centerRepository.findById({})...", centerId);
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));
        LOGGER.info("  center found: id={}, name={}", center.getId(), center.getName());

        LOGGER.info("[STEP 4] assertOwnerAndCenterMembership(currentUser, centerId)...");
        assertOwnerAndCenterMembership(currentUser, centerId);
        LOGGER.info("  PASSED");

        LOGGER.info("[STEP 5] Email uniqueness check...");
        LOGGER.info("  request.email = '{}'", request.getEmail());
        LOGGER.info("  email != null? {}", request.getEmail() != null);
        LOGGER.info("  email.isBlank()? {}", request.getEmail() != null && request.getEmail().isBlank());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            boolean emailExists = userRepository.existsByEmail(request.getEmail());
            LOGGER.info("  existsByEmail('{}') = {}", request.getEmail(), emailExists);
            if (emailExists) {
                LOGGER.error("  >>> THROWING DuplicateResourceException: Email '{}' already exists", request.getEmail());
                throw new DuplicateResourceException("Email already exists");
            }
            LOGGER.info("  PASSED: email is unique");
        } else {
            LOGGER.info("  SKIPPED: email is null or blank");
        }

        User teacherUser;
        String temporaryPassword = null;

        LOGGER.info("[STEP 6] Phone lookup...");
        LOGGER.info("  findByPhoneNumber('{}')...", request.getPhoneNumber());
        var existingUser = userRepository.findByPhoneNumber(request.getPhoneNumber());
        LOGGER.info("  existingUser.isPresent() = {}", existingUser.isPresent());
        if (existingUser.isPresent()) {
            teacherUser = existingUser.get();
            LOGGER.info("  existing user: id={}, phone={}, role={}", teacherUser.getId(), teacherUser.getPhoneNumber(), teacherUser.getRole());

            LOGGER.info("[STEP 6a] Role check: role == TEACHER?");
            if (teacherUser.getRole() != Role.TEACHER) {
                LOGGER.error("  >>> THROWING BadRequestException: User role is {}, not TEACHER", teacherUser.getRole());
                throw new BadRequestException("User is not TEACHER");
            }
            LOGGER.info("  PASSED: role is TEACHER");

            LOGGER.info("[STEP 6b] Membership check...");
            boolean existsMembership = membershipRepository.existsByUser_IdAndCenter_Id(teacherUser.getId(), centerId);
            LOGGER.info("  existsByUser_IdAndCenter_Id({}, {}) = {}", teacherUser.getId(), centerId, existsMembership);
            if (!existsMembership) {
                LOGGER.info("  Creating membership...");
                createMembership(teacherUser, center, currentUser);
            }
        } else {
            LOGGER.info("  User does not exist, will create new...");
            temporaryPassword = generateTemporaryPassword();
            LOGGER.info("  generated temporaryPassword (length={})", temporaryPassword.length());

            teacherUser = new User();
            teacherUser.setPhoneNumber(request.getPhoneNumber());
            teacherUser.setEmail(request.getEmail());
            teacherUser.setFullName(request.getFullName());
            teacherUser.setRole(Role.TEACHER);
            teacherUser.setPassword(passwordEncoder.encode(temporaryPassword));
            LOGGER.info("  saving new user...");
            teacherUser = userRepository.save(teacherUser);
            LOGGER.info("  saved: id={}, phone={}", teacherUser.getId(), teacherUser.getPhoneNumber());

            LOGGER.info("  creating membership...");
            createMembership(teacherUser, center, currentUser);
        }

        LOGGER.info("[STEP 7] applyPermissionOverridesIfPresent...");
        applyPermissionOverridesIfPresent(teacherUser.getId(), request.getPermissionOverrides());

        boolean includeSalary = currentUser.getRole() == Role.OWNER;
        LOGGER.info("[STEP 8] toResponse (includeSalary={}, temporaryPassword={})", includeSalary, temporaryPassword != null ? "***" : "null");

        TeacherResponse response = toResponse(teacherUser, centerId, temporaryPassword, includeSalary);
        LOGGER.info("=== TeacherService.create() END — SUCCESS (userId={}) ===", response.getUserId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        boolean includeSalary = currentUser.getRole() == Role.OWNER;

        return membershipRepository.findAllByCenter_IdAndUserRole(centerId, Role.TEACHER)
                .stream()
                .map(Membership::getUser)
                .map(user -> toResponse(user, centerId, null, includeSalary))
                .toList();
    }

    // Update
    @Transactional
    public TeacherResponse update(Long teacherId, TeacherRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(teacherId, centerId, Role.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found in this center"));

        User teacher = membership.getUser();

        String phoneNumber = request.getPhoneNumber().trim();
        String email = normalizeOptionalEmail(request.getEmail());
        String fullName = request.getFullName().trim();

        if (!teacher.getPhoneNumber().equals(phoneNumber)
                && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        if (email != null
                && !email.equals(teacher.getEmail())
                && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        teacher.setPhoneNumber(phoneNumber);
        teacher.setEmail(email);
        teacher.setFullName(fullName);

        teacher = userRepository.save(teacher);

        applyPermissionOverridesIfPresent(teacher.getId(), request.getPermissionOverrides());

        boolean includeSalary = currentUser.getRole() == Role.OWNER;

        return toResponse(teacher, centerId, null, includeSalary);
    }

    // Delete
    @Transactional
    public void delete(Long teacherId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUser_IdAndCenter_IdAndUserRole(teacherId, centerId, Role.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found in this center"));

        membershipRepository.delete(membership);
    }

    @Transactional
    public List<BulkTeacherResult> bulkCreate(@NonNull BulkTeacherRequest request) {

        if (request.getTeachers() == null || request.getTeachers().isEmpty()) {
            throw new BadRequestException("Teachers list must not be empty");
        }

        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        assertOwnerAndCenterMembership(currentUser, centerId);

        List<BulkTeacherError> errors = new ArrayList<>();

        // PHASE 1: VALIDATION ONLY
        for (int i = 0; i < request.getTeachers().size(); i++) {

            BulkTeacherRequest.Item item = request.getTeachers().get(i);

            int row = i + 1;

            // 1. Validate required fields
            if (item.getPhoneNumber() == null || item.getPhoneNumber().isBlank()
                    || item.getFullName() == null || item.getFullName().isBlank()) {

                BulkTeacherError error = new BulkTeacherError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus(BulkTeacherStatus.INVALID_INPUT);
                error.setMessage("Phone number and full name are required");

                errors.add(error);
                continue;
            }

            // 2. Check duplicate email
            if (item.getEmail() != null && !item.getEmail().isBlank()
                    && userRepository.existsByEmail(item.getEmail())) {

                BulkTeacherError error = new BulkTeacherError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus(BulkTeacherStatus.INVALID_INPUT);
                error.setMessage("Email already exists");

                errors.add(error);
                continue;
            }

            // 3. Check role conflict
            var existingUser = userRepository.findByPhoneNumber(item.getPhoneNumber());

            if (existingUser.isPresent() && existingUser.get().getRole() != Role.TEACHER) {

                BulkTeacherError error = new BulkTeacherError();
                error.setRow(row);
                error.setPhoneNumber(item.getPhoneNumber());
                error.setStatus(BulkTeacherStatus.INVALID_INPUT);
                error.setMessage("Existing User role is not TEACHER");

                errors.add(error);
                continue;
            }
        }

        // If there is an error -> throw everything
        if (!errors.isEmpty()) {
            throw new BulkTeacherValidationException(errors);
        }

        // PHASE 2: EXECUTION
        List<BulkTeacherResult> results = new ArrayList<>();

        for (BulkTeacherRequest.Item item : request.getTeachers()) {

            var existingUser = userRepository.findByPhoneNumber(item.getPhoneNumber());

            // Case: existing teacher
            if (existingUser.isPresent()) {
                User teacherUser = existingUser.get();

                boolean existsMembership =
                        membershipRepository.existsByUser_IdAndCenter_Id(teacherUser.getId(), centerId);

                BulkTeacherResult result = new BulkTeacherResult();
                result.setPhoneNumber(item.getPhoneNumber());
                result.setTemporaryPassword(null);

                if (existsMembership) {
                    result.setStatus(BulkTeacherStatus.ALREADY_IN_CENTER);
                } else {
                    createMembership(teacherUser, center, currentUser);
                    result.setStatus(BulkTeacherStatus.ADDED_TO_CENTER);
                }

                results.add(result);
                continue;
            }

            // Case: new teacher
            String temporaryPassword = generateTemporaryPassword();

            User teacherUser = new User();
            teacherUser.setPhoneNumber(item.getPhoneNumber());
            teacherUser.setEmail(item.getEmail());
            teacherUser.setFullName(item.getFullName());
            teacherUser.setRole(Role.TEACHER);
            teacherUser.setPassword(passwordEncoder.encode(temporaryPassword));

            teacherUser = userRepository.save(teacherUser);

            createMembership(teacherUser, center, currentUser);

            BulkTeacherResult result = new BulkTeacherResult();
            result.setPhoneNumber(item.getPhoneNumber());
            result.setStatus(BulkTeacherStatus.CREATED);
            result.setTemporaryPassword(temporaryPassword);

            results.add(result);
        }

        return results;
    }

    //HELPER FUNCTION

    private void createMembership(User teacherUser, Center center, User joinedByUser) {
        Membership membership = new Membership();
        membership.setUser(teacherUser);
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

    private TeacherResponse toResponse(User user, Long centerId, String temporaryPassword, boolean includeSalary) {
        TeacherResponse.TeacherResponseBuilder builder = TeacherResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .centerId(centerId)
                .temporaryPassword(temporaryPassword);

        if (includeSalary) {
            TeacherCenterProfile profile = teacherCenterProfileRepository
                    .findByTeacher_IdAndCenter_Id(user.getId(), centerId)
                    .orElse(null);

            if (profile != null) {
                builder.salary(profile.getSalary());
                builder.currency(profile.getCurrency());
            }
        }

        return builder.build();
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
            throw new AccessDeniedException("Only OWNER can add teacher to center");
        }

        assertCenterMembership(currentUser, centerId);
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
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
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }

        email = email.trim().toLowerCase();

        return email.isEmpty() ? null : email;
    }
}
