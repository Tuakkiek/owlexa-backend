package com.owlexa.owlexabackend.modules.enrollment.service;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleEventRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRecurringRuleRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;

    @Value("${app.enrollment.fee-grace-days:7}")
    private int feeGraceDays;

    @Transactional
    public EnrollmentResponse enroll (Long classId, EnrollmentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser,centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        if (clazz.getStatus() != com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus.ACTIVE) {
            throw new BusinessRuleException("Lớp học không mở đăng ký. Trạng thái hiện tại: " + clazz.getStatus());
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + request.getStudentId()
                ));

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Người dùng không phải là học sinh");
        }

        // Check for existing enrollment record
        var existingEnrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, student.getId());

        if (existingEnrollment.isPresent()) {
            ClassEnrollment enrollment = existingEnrollment.get();

            switch (enrollment.getStatus()) {
                case ACTIVE:
                case PENDING:
                case SUSPENDED:
                    throw new DuplicateResourceException(
                            "Học sinh đã đăng ký lớp học này trước đó. Trạng thái hiện tại: " + enrollment.getStatus()
                    );
                case DROPPED:
                    // Restore the dropped enrollment - check capacity first
                    long activeCount = classEnrollmentRepository.countByClazz_IdAndStatusIn(
                            classId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED)
                    );
                    if (activeCount >= resolveCapacityLimit(clazz, centerId)) {
                        throw new BusinessRuleException("Lớp học đã đạt sức chứa phòng học");
                    }

                    validateStudentScheduleConflicts(clazz, student, centerId);

                    // Restore: set status to ACTIVE, update enrolled-by user
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollment.setEnrolledByUser(currentUser);
                    enrollment = classEnrollmentRepository.save(enrollment);

                    // Regenerate fee record if needed (the method already checks existence)
                    generateFeeRecordIfAbsent(enrollment);

                    return toResponse(enrollment);
            }
        }

        // No existing enrollment - check capacity and create new
        long pendingOrActiveCount = classEnrollmentRepository.countByClazz_IdAndStatusIn(
                classId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED)
        );

        if (pendingOrActiveCount >= resolveCapacityLimit(clazz, centerId)) {
            throw new BusinessRuleException("Lớp học đã đạt sức chứa phòng học");
        }

        validateStudentScheduleConflicts(clazz, student, centerId);

        // Check student schedule conflicts
        List<com.owlexa.owlexabackend.modules.class_management.entity.Schedule> classSchedules =
                scheduleRepository.findAllByClazz_IdAndCenter_Id(classId, centerId);
        for (var schedule : classSchedules) {
            List<com.owlexa.owlexabackend.modules.class_management.entity.Schedule> overlaps =
                    scheduleRepository.findOverlappingStudentSchedules(
                            student.getId(), schedule.getDayOfWeek(),
                            schedule.getStartTime(), schedule.getEndTime(), centerId, null);
            if (!overlaps.isEmpty()) {
                throw new BusinessRuleException(
                        "STUDENT_CONFLICT",
                        String.format("Học sinh %s đã có lịch học lớp khác vào thời gian này.",
                                student.getFullName())
                );
            }
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .clazz(clazz)
                .studentUser(student)
                .center(clazz.getCenter())
                .enrolledByUser(currentUser)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollment = classEnrollmentRepository.save(enrollment);

        // Immediately generate the first month's fee record (Owner-initiated flow)
        generateFeeRecordIfAbsent(enrollment);

        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse approve(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin đăng ký cho học sinh có ID: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể duyệt các yêu cầu đăng ký đang chờ xử lý. Trạng thái hiện tại: " + enrollment.getStatus());
        }

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = classEnrollmentRepository.save(enrollment);

        // Auto-generate FeeRecord
        generateFeeRecordIfAbsent(enrollment);

        return toResponse(enrollment);
    }

    @Transactional
    public void reject(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin đăng ký cho học sinh có ID: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessRuleException("Chỉ có thể từ chối các yêu cầu đăng ký đang chờ xử lý. Trạng thái hiện tại: " + enrollment.getStatus());
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        classEnrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAllByClass(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        return classEnrollmentRepository.findAllByClazz_IdAndStatusIn(
                        classId, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all DROPPED (withdrawn) enrollments for a class.
     * Used by the owner to view and optionally restore accidentally removed students.
     * Historical data (fees, attendance) is fully preserved for all DROPPED records.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findDroppedByClass(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        return classEnrollmentRepository.findAllByClazz_IdAndStatusIn(
                        classId, List.of(EnrollmentStatus.DROPPED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void drop (Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));
        if(!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp học này không thuộc trung tâm hiện tại.");
        }

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ghi danh của học viên ID: " + studentUserId
                ));
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return;
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        classEnrollmentRepository.save(enrollment);
    }

    /**
     * Suspend an active enrollment (e.g., for non-payment).
     * Only reachable from ACTIVE. Preserves the enrollment record and all history.
     * Called by the overdue scheduler or by Owner as a manual override.
     */
    @Transactional
    public void suspend(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ghi danh của học viên ID: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Chỉ có thể tạm ngưng ghi danh đang hoạt động. Trạng thái hiện tại: " + enrollment.getStatus()
            );
        }

        enrollment.setStatus(EnrollmentStatus.SUSPENDED);
        classEnrollmentRepository.save(enrollment);
    }

    /**
     * Reactivate a suspended enrollment (e.g., after payment is received).
     * Only reachable from SUSPENDED. Called by PaymentService after a payment
     * clears the overdue condition, or by Owner as a manual override.
     */
    @Transactional
    public void reactivate(Long classId, Long studentUserId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ghi danh của học viên ID: " + studentUserId
                ));

        if (enrollment.getStatus() != EnrollmentStatus.SUSPENDED) {
            throw new BusinessRuleException(
                    "Chỉ có thể kích hoạt lại ghi danh đang tạm ngưng. Trạng thái hiện tại: " + enrollment.getStatus()
            );
        }

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        classEnrollmentRepository.save(enrollment);
    }

    private int resolveCapacityLimit(Class clazz, Long centerId) {
        Integer eventRoomCapacity = scheduleEventRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);
        Integer ruleRoomCapacity = scheduleRecurringRuleRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);
        Integer legacyRoomCapacity = scheduleRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);

        return List.of(eventRoomCapacity, ruleRoomCapacity, legacyRoomCapacity).stream()
                .filter(value -> value != null && value > 0)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private void validateStudentScheduleConflicts(Class clazz, User student, Long centerId) {
        Long classId = clazz.getId();

        var classEvents = scheduleEventRepository.findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(classId, centerId);
        for (var event : classEvents) {
            if (event.getStatus() == com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus.CANCELLED) {
                continue;
            }
            assertNoLegacyStudentConflict(student, event.getEventDate().getDayOfWeek(), event.getStartTime(), event.getEndTime(), centerId);
            if (!scheduleEventRepository.findOverlappingStudentEvents(
                    centerId,
                    student.getId(),
                    event.getEventDate(),
                    event.getStartTime(),
                    event.getEndTime(),
                    com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus.CANCELLED,
                    event.getId()
            ).isEmpty()) {
                throwStudentConflict(student);
            }
            assertNoRuleStudentConflict(student, event.getEventDate().getDayOfWeek(), event.getStartTime(), event.getEndTime(), centerId, classId);
        }

        var classRules = scheduleRecurringRuleRepository.findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(classId, centerId);
        for (var rule : classRules) {
            if (!Boolean.TRUE.equals(rule.getIsActive())) {
                continue;
            }
            for (Integer day : parseDays(rule.getDaysOfWeek())) {
                DayOfWeek dayOfWeek = DayOfWeek.of(day);
                assertNoLegacyStudentConflict(student, dayOfWeek, rule.getStartTime(), rule.getEndTime(), centerId);
                assertNoRuleStudentConflict(student, dayOfWeek, rule.getStartTime(), rule.getEndTime(), centerId, classId);
            }
        }
    }

    private void assertNoLegacyStudentConflict(
            User student,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId
    ) {
        if (!scheduleRepository.findOverlappingStudentSchedules(student.getId(), dayOfWeek, startTime, endTime, centerId, null).isEmpty()) {
            throwStudentConflict(student);
        }
    }

    private void assertNoRuleStudentConflict(
            User student,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long centerId,
            Long targetClassId
    ) {
        Set<Long> enrolledClassIds = classEnrollmentRepository
                .findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                        student.getId(),
                        centerId,
                        List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING, EnrollmentStatus.SUSPENDED))
                .stream()
                .map(enrollment -> enrollment.getClazz().getId())
                .filter(enrolledClassId -> !enrolledClassId.equals(targetClassId))
                .collect(Collectors.toSet());

        if (enrolledClassIds.isEmpty()) {
            return;
        }

        for (var rule : scheduleRecurringRuleRepository.findAllByCenter_IdAndIsActiveTrue(centerId)) {
            if (!enrolledClassIds.contains(rule.getClazz().getId())) {
                continue;
            }
            if (parseDays(rule.getDaysOfWeek()).contains(dayOfWeek.getValue())
                    && startTime.isBefore(rule.getEndTime())
                    && endTime.isAfter(rule.getStartTime())) {
                throwStudentConflict(student);
            }
        }
    }

    private Set<Integer> parseDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    private void throwStudentConflict(User student) {
        throw new BusinessRuleException(
                "STUDENT_CONFLICT",
                String.format("Há»c sinh %s Ä‘Ã£ cÃ³ lá»‹ch há»c lá»›p khÃ¡c vÃ o thá»i gian nÃ y.",
                        student.getFullName())
        );
    }

    // Helper: auto-generate FeeRecord when enrollment becomes ACTIVE
    private void generateFeeRecordIfAbsent(ClassEnrollment enrollment) {
        String currentMonth = YearMonth.now().toString();

        boolean alreadyExists = feeRecordRepository
                .findByStudentUser_IdAndClazz_IdAndMonth(
                        enrollment.getStudentUser().getId(),
                        enrollment.getClazz().getId(),
                        currentMonth
                ).isPresent();

        if (!alreadyExists && enrollment.getClazz().getMonthlyFee() != null
                && enrollment.getClazz().getMonthlyFee() > 0) {
            FeeRecord feeRecord = FeeRecord.builder()
                    .studentUser(enrollment.getStudentUser())
                    .center(enrollment.getCenter())
                    .clazz(enrollment.getClazz())
                    .amount(BigDecimal.valueOf(enrollment.getClazz().getMonthlyFee()))
                    .paidAmount(BigDecimal.ZERO)
                    .month(currentMonth)
                    .dueDate(LocalDate.now().plusDays(feeGraceDays))
                    .status(FeeStatus.UNPAID)
                    .build();
            feeRecordRepository.save(feeRecord);
        }
    }

    // To response
    private EnrollmentResponse toResponse(ClassEnrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .classId(enrollment.getClazz().getId())
                .centerId(enrollment.getCenter().getId())
                .studentUserId(enrollment.getStudentUser().getId())
                .studentPhoneNumber(enrollment.getStudentUser().getPhoneNumber())
                .studentFullName(enrollment.getStudentUser().getFullName())
                .enrollmentByUserId(enrollment.getEnrolledByUser() != null ? enrollment.getEnrolledByUser().getId() : null)
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("User not authenticated");
        }

        String phoneNumber = authentication.getName();

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại."));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();

        if (centerId == null) {
            throw new BadRequestException("Không xác định được trung tâm hiện tại. Vui lòng kiểm tra quyền truy cập.");
        }

        return centerId;
    }

    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Chỉ chủ trung tâm mới có thể quản lý ghi danh.");
        }

        assertCenterMembership(currentUser, centerId);
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);

        if (!hasMembership) {
            throw new AccessDeniedException("Người dùng không thuộc trung tâm hiện tại.");
        }
    }
}
