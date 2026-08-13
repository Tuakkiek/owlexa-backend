package com.owlexa.owlexabackend.modules.enrollment.service;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.DropEnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.EnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.request.TransferEnrollmentRequest;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.EnrollmentResponse;
import com.owlexa.owlexabackend.modules.enrollment.dto.response.TransferResponse;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.DropReason;
import com.owlexa.owlexabackend.modules.payment.entity.AuditLog;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.FeeStatus;
import com.owlexa.owlexabackend.modules.payment.entity.Refund;
import com.owlexa.owlexabackend.modules.payment.entity.RefundStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEvent;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleEventStatus;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleRecurringRule;
import com.owlexa.owlexabackend.modules.class_management.entity.ScheduleType;
import com.owlexa.owlexabackend.modules.class_management.entity.Schedule;
import com.owlexa.owlexabackend.modules.user.entity.Center;
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
import com.owlexa.owlexabackend.modules.payment.repository.AuditLogRepository;
import com.owlexa.owlexabackend.modules.payment.repository.FeeRecordRepository;
import com.owlexa.owlexabackend.modules.payment.repository.PaymentRepository;
import com.owlexa.owlexabackend.modules.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.time.YearMonth;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final ScheduleRecurringRuleRepository scheduleRecurringRuleRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

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
        var existingEnrollment = findEnrollment(classId, student.getId());

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
                    // Restore the dropped/transferred enrollment - check capacity first
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
                    enrollment.setDropReason(null);
                    enrollment.setDroppedAt(null);
                    enrollment.setDroppedByUser(null);
                    enrollment = classEnrollmentRepository.save(enrollment);

                    // A drop cancels every unpaid fee for this enrollment. When
                    // the student is enrolled again, restore all of those fee
                    // rows, not only the current month; otherwise the student
                    // portal can show old fees that cashier cannot collect.
                    reactivateCancelledUnpaidFeeRecords(enrollment);

                    // Generate the current month's fee if it does not exist.
                    generateFeeRecordIfAbsent(enrollment);

                    return toResponse(enrollment);
                case TRANSFERRED:
                    throw new BusinessRuleException(
                            "Enrollment has already been transferred and cannot be restored in the old class.");
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

        // A PENDING enrollment may have been created before the class schedule
        // changed. Re-check at approval time so approval cannot bypass the
        // same conflict rules as direct owner enrollment.
        validateStudentScheduleConflicts(clazz, enrollment.getStudentUser(), centerId);

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
        enrollment.setDropReason(DropReason.OTHER);
        enrollment.setDroppedAt(Instant.now());
        enrollment.setDroppedByUser(currentUser);
        classEnrollmentRepository.save(enrollment);
        cancelUnpaidFeeRecords(enrollment);
        BigDecimal feeDiff = calculateDropFeeDifference(enrollment, centerId);
        if (feeDiff.compareTo(BigDecimal.ZERO) > 0) {
            autoCreateRefundRequest(enrollment, feeDiff, currentUser,
                    "Auto-created for legacy drop endpoint");
        }
        writeAuditLog(currentUser, enrollment.getCenter(), "ENROLLMENT_DROP", "ClassEnrollment",
                enrollment.getId(), "Enrollment dropped from class " + classId);
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

        Class clazz = enrollment.getClazz();
        if (clazz == null || clazz.getCenter() == null || !centerId.equals(clazz.getCenter().getId())) {
            throw new TenancyViolationException("Ghi danh không thuộc trung tâm hiện tại.");
        }
        validateStudentScheduleConflicts(clazz, enrollment.getStudentUser(), centerId);

        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        classEnrollmentRepository.save(enrollment);
    }

    private int resolveCapacityLimit(Class clazz, Long centerId) {
        Integer eventRoomCapacity = scheduleEventRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);
        Integer ruleRoomCapacity = scheduleRecurringRuleRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);
        Integer legacyRoomCapacity = scheduleRepository.findMinRoomCapacityByClass(clazz.getId(), centerId);

        // A class may only use one scheduling model, so the other capacity
        // queries legitimately return null. Stream.of accepts those values;
        // List.of throws before the null filter can run.
        return java.util.stream.Stream.of(eventRoomCapacity, ruleRoomCapacity, legacyRoomCapacity)
                .filter(value -> value != null && value > 0)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * Validates the complete schedule of a class before a student is enrolled.
     *
     * The project currently supports three schedule representations. They must
     * be compared against each other, otherwise a recurring rule can be missed
     * or an old schedule row can cause a false positive. A legacy weekly slot
     * has an open-ended date range; a recurring rule has its own date range; an
     * event applies only to its exact date.
     */
    private void validateStudentScheduleConflicts(Class clazz, User student, Long centerId) {
        validateStudentScheduleConflicts(clazz, student, centerId, null);
    }

    private void validateStudentScheduleConflicts(Class clazz, User student, Long centerId, Long excludedClassId) {
        Long targetClassId = clazz.getId();
        List<StudentScheduleSlot> existingSlots = loadStudentScheduleSlots(
                student, centerId, targetClassId, excludedClassId);

        if (existingSlots.isEmpty()) {
            return;
        }

        for (Schedule schedule : scheduleRepository.findAllByClazz_IdAndCenter_Id(targetClassId, centerId)) {
            if (schedule.getType() == ScheduleType.CANCELLED
                    || schedule.getDayOfWeek() == null
                    || schedule.getStartTime() == null
                    || schedule.getEndTime() == null) {
                continue;
            }
            assertNoConflict(student, existingSlots, new StudentScheduleSlot(
                    schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), null, null));
        }

        for (ScheduleRecurringRule rule : scheduleRecurringRuleRepository
                .findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(targetClassId, centerId)) {
            if (!isUsableRecurringRule(rule)) {
                continue;
            }
            for (Integer day : parseDays(rule.getDaysOfWeek())) {
                assertNoConflict(student, existingSlots, new StudentScheduleSlot(
                        DayOfWeek.of(day), rule.getStartTime(), rule.getEndTime(),
                        rule.getStartDate(), rule.getEndDate()));
            }
        }

        for (ScheduleEvent event : scheduleEventRepository
                .findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(targetClassId, centerId)) {
            if (event.getStatus() == ScheduleEventStatus.CANCELLED
                    || event.getEventDate() == null
                    || event.getStartTime() == null
                    || event.getEndTime() == null) {
                continue;
            }
            assertNoConflict(student, existingSlots, new StudentScheduleSlot(
                    event.getEventDate().getDayOfWeek(), event.getStartTime(), event.getEndTime(),
                    event.getEventDate(), event.getEventDate()));
        }
    }

    private List<StudentScheduleSlot> loadStudentScheduleSlots(
            User student, Long centerId, Long targetClassId, Long excludedClassId) {
        List<StudentScheduleSlot> slots = new ArrayList<>();
        List<ClassEnrollment> enrollments = classEnrollmentRepository
                .findAllByStudentUser_IdAndCenter_IdAndStatusIn(
                        student.getId(), centerId,
                        List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING, EnrollmentStatus.SUSPENDED));

        for (ClassEnrollment enrollment : enrollments) {
            if (enrollment.getClazz() == null || enrollment.getClazz().getId() == null) {
                continue;
            }
            Long enrolledClassId = enrollment.getClazz().getId();
            if (enrolledClassId.equals(targetClassId)
                    || (excludedClassId != null && enrolledClassId.equals(excludedClassId))) {
                continue;
            }

            for (Schedule schedule : scheduleRepository.findAllByClazz_IdAndCenter_Id(enrolledClassId, centerId)) {
                if (schedule.getType() != ScheduleType.CANCELLED
                        && schedule.getDayOfWeek() != null
                        && schedule.getStartTime() != null
                        && schedule.getEndTime() != null) {
                    slots.add(new StudentScheduleSlot(
                            schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime(), null, null));
                }
            }

            for (ScheduleRecurringRule rule : scheduleRecurringRuleRepository
                    .findAllByClazz_IdAndCenter_IdOrderByStartDateAscStartTimeAsc(enrolledClassId, centerId)) {
                if (!isUsableRecurringRule(rule)) {
                    continue;
                }
                for (Integer day : parseDays(rule.getDaysOfWeek())) {
                    slots.add(new StudentScheduleSlot(
                            DayOfWeek.of(day), rule.getStartTime(), rule.getEndTime(),
                            rule.getStartDate(), rule.getEndDate()));
                }
            }

            for (ScheduleEvent event : scheduleEventRepository
                    .findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(enrolledClassId, centerId)) {
                if (event.getStatus() != ScheduleEventStatus.CANCELLED
                        && event.getEventDate() != null
                        && event.getStartTime() != null
                        && event.getEndTime() != null) {
                    slots.add(new StudentScheduleSlot(
                            event.getEventDate().getDayOfWeek(), event.getStartTime(), event.getEndTime(),
                            event.getEventDate(), event.getEventDate()));
                }
            }
        }
        return slots;
    }

    private void assertNoConflict(User student, List<StudentScheduleSlot> existingSlots,
                                  StudentScheduleSlot targetSlot) {
        if (targetSlot.startTime() == null || targetSlot.endTime() == null
                || !targetSlot.startTime().isBefore(targetSlot.endTime())) {
            return;
        }
        if (existingSlots.stream().anyMatch(existingSlot -> slotsOverlap(existingSlot, targetSlot))) {
            throwStudentConflict(student);
        }
    }

    private boolean slotsOverlap(StudentScheduleSlot first, StudentScheduleSlot second) {
        return first.dayOfWeek() == second.dayOfWeek()
                && first.startTime().isBefore(second.endTime())
                && first.endTime().isAfter(second.startTime())
                && dateRangesOverlap(first.startDate(), first.endDate(), second.startDate(), second.endDate());
    }

    private boolean dateRangesOverlap(LocalDate firstStart, LocalDate firstEnd,
                                      LocalDate secondStart, LocalDate secondEnd) {
        return (firstEnd == null || secondStart == null || !firstEnd.isBefore(secondStart))
                && (secondEnd == null || firstStart == null || !secondEnd.isBefore(firstStart));
    }

    private boolean isUsableRecurringRule(ScheduleRecurringRule rule) {
        return rule != null
                && Boolean.TRUE.equals(rule.getIsActive())
                && rule.getType() != ScheduleType.CANCELLED
                && rule.getStartDate() != null
                && rule.getEndDate() != null
                && !rule.getStartDate().isAfter(rule.getEndDate())
                && rule.getStartTime() != null
                && rule.getEndTime() != null;
    }

    private record StudentScheduleSlot(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate) {
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
                String.format("Học sinh %s đã có lịch học lớp khác vào thời gian này.",
                        student.getFullName())
        );
    }

    private Optional<ClassEnrollment> findEnrollment(Long classId, Long studentUserId) {
        return classEnrollmentRepository.findByClazz_IdAndStudentUser_Id(classId, studentUserId);
    }

    /** Cancel only fee rows that have not received money yet. Paid/partial rows
     * remain auditable and are handled by the refund workflow below. */
    private void cancelUnpaidFeeRecords(ClassEnrollment enrollment) {
        feeRecordRepository.findAllByStudentUser_IdAndClazz_Id(
                        enrollment.getStudentUser().getId(), enrollment.getClazz().getId())
                .stream()
                .filter(record -> record.getStatus() == FeeStatus.UNPAID
                        || record.getStatus() == FeeStatus.PARTIAL)
                .filter(record -> record.getPaidAmount() == null
                        || record.getPaidAmount().compareTo(BigDecimal.ZERO) == 0)
                .forEach(record -> record.setStatus(FeeStatus.CANCELLED));
    }

    private void reactivateCancelledUnpaidFeeRecords(ClassEnrollment enrollment) {
        feeRecordRepository.findAllByStudentUser_IdAndClazz_Id(
                        enrollment.getStudentUser().getId(), enrollment.getClazz().getId())
                .stream()
                .filter(record -> record.getStatus() == FeeStatus.CANCELLED)
                .filter(record -> record.getPaidAmount() == null
                        || record.getPaidAmount().compareTo(BigDecimal.ZERO) == 0)
                .forEach(record -> {
                    record.setStatus(FeeStatus.UNPAID);
                    feeRecordRepository.save(record);
                });
    }

    // Helper: auto-generate FeeRecord when enrollment becomes ACTIVE
    private void generateFeeRecordIfAbsent(ClassEnrollment enrollment) {
        String currentMonth = YearMonth.now().toString();

        BigDecimal feeAmount = resolveMonthlyFee(enrollment.getClazz());

        Optional<FeeRecord> existingFeeRecord = feeRecordRepository
                .findByStudentUser_IdAndClazz_IdAndMonth(
                        enrollment.getStudentUser().getId(),
                        enrollment.getClazz().getId(),
                        currentMonth
                );

        if (existingFeeRecord.isPresent()) {
            FeeRecord feeRecord = existingFeeRecord.get();

            // Dropping an enrollment cancels an unpaid fee row so it no longer
            // appears in the cashier's pending list. Re-enrolling the same
            // student must reactivate that row instead of treating it as an
            // already-complete fee record. Paid history is never reset here.
            if (feeRecord.getStatus() == FeeStatus.CANCELLED
                    && (feeRecord.getPaidAmount() == null
                    || feeRecord.getPaidAmount().compareTo(BigDecimal.ZERO) == 0)
                    && feeAmount != null) {
                feeRecord.setAmount(feeAmount);
                feeRecord.setPaidAmount(BigDecimal.ZERO);
                feeRecord.setDueDate(LocalDate.now().plusDays(feeGraceDays));
                feeRecord.setStatus(FeeStatus.UNPAID);
                feeRecordRepository.save(feeRecord);
                log.info("Reactivated enrollment fee: studentId={}, classId={}, month={}, amount={}",
                        enrollment.getStudentUser().getId(), enrollment.getClazz().getId(), currentMonth, feeAmount);
            }
            return;
        }

        if (feeAmount != null) {
            FeeRecord feeRecord = FeeRecord.builder()
                    .studentUser(enrollment.getStudentUser())
                    .center(enrollment.getCenter())
                    .clazz(enrollment.getClazz())
                    .amount(feeAmount)
                    .paidAmount(BigDecimal.ZERO)
                    .month(currentMonth)
                    .dueDate(LocalDate.now().plusDays(feeGraceDays))
                    .status(FeeStatus.UNPAID)
                    .build();
            feeRecordRepository.save(feeRecord);
            log.info("Created enrollment fee: studentId={}, classId={}, month={}, amount={}",
                    enrollment.getStudentUser().getId(), enrollment.getClazz().getId(), currentMonth, feeAmount);
        } else {
            log.warn("Skipped enrollment fee: classId={} has no positive monthly fee configured",
                    enrollment.getClazz().getId());
        }
    }

    private BigDecimal resolveMonthlyFee(Class clazz) {
        Double monthlyFee = clazz.getMonthlyFee();
        if ((monthlyFee == null || monthlyFee <= 0)
                && clazz.getCourse() != null) {
            monthlyFee = clazz.getCourse().getDefaultMonthlyFee();
        }
        return monthlyFee != null && monthlyFee > 0
                ? BigDecimal.valueOf(monthlyFee)
                : null;
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
                .dropReason(enrollment.getDropReason())
                .droppedAt(enrollment.getDroppedAt())
                .transferredToEnrollmentId(enrollment.getTransferredToEnrollment() != null ? enrollment.getTransferredToEnrollment().getId() : null)
                .transferredFromEnrollmentId(enrollment.getTransferredFromEnrollment() != null ? enrollment.getTransferredFromEnrollment().getId() : null)
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

    // ── Drop with reason ──────────────────────────────────────────────────

    @Transactional
    public EnrollmentResponse dropWithReason(Long classId, Long studentUserId, DropEnrollmentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);

        ClassEnrollment enrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ghi danh của học viên ID: " + studentUserId));

        // Idempotent: already dropped
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return toResponse(enrollment);
        }

        if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
            throw new BusinessRuleException("Ghi danh đã được chuyển sang lớp khác, không thể nghỉ ngang trực tiếp.");
        }

        // Allow drop from ACTIVE and SUSPENDED
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
                && enrollment.getStatus() != EnrollmentStatus.SUSPENDED) {
            throw new BusinessRuleException(
                    "Chỉ có thể nghỉ ngang ghi danh ACTIVE hoặc SUSPENDED. Trạng thái hiện tại: " + enrollment.getStatus());
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setDropReason(request.getReason());
        enrollment.setDroppedAt(Instant.now());
        enrollment.setDroppedByUser(currentUser);
        classEnrollmentRepository.save(enrollment);
        cancelUnpaidFeeRecords(enrollment);

        // Calculate fee difference and auto-create Refund REQUESTED if student overpaid
        BigDecimal feeDiff = calculateDropFeeDifference(enrollment, centerId);
        if (feeDiff.compareTo(BigDecimal.ZERO) > 0) {
            autoCreateRefundRequest(enrollment, feeDiff, currentUser,
                    "Tự động tạo do nghỉ ngang - Lý do: " + request.getReason());
        }

        writeAuditLog(currentUser, enrollment.getCenter(), "ENROLLMENT_DROP", "ClassEnrollment",
                enrollment.getId(), "Nghỉ ngang lớp " + classId + ", lý do: " + request.getReason()
                        + (request.getNote() != null ? ", ghi chú: " + request.getNote() : ""));

        return toResponse(enrollment);
    }

    // ── Transfer ──────────────────────────────────────────────────────────

    @Transactional
    public TransferResponse transfer(Long classId, Long studentUserId, TransferEnrollmentRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);

        ClassEnrollment oldEnrollment = classEnrollmentRepository
                .findByClazz_IdAndStudentUser_Id(classId, studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh."));

        if (oldEnrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ chuyển được ghi danh đang ACTIVE. Trạng thái hiện tại: "
                    + oldEnrollment.getStatus());
        }

        Class targetClass = classRepository.findById(request.getTargetClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp đích."));

        if (!targetClass.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Lớp đích không thuộc trung tâm hiện tại.");
        }
        if (targetClass.getId().equals(classId)) {
            throw new BusinessRuleException("Lớp đích phải khác lớp hiện tại.");
        }

        // Check capacity of target class
        long activeCount = classEnrollmentRepository.countByClazz_IdAndStatusIn(
                request.getTargetClassId(),
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE, EnrollmentStatus.SUSPENDED));
        if (activeCount >= resolveCapacityLimit(targetClass, centerId)) {
            throw new BusinessRuleException("Lớp đích đã đạt sức chứa tối đa.");
        }

        // Check schedule conflicts for target class
        User student = oldEnrollment.getStudentUser();
        validateStudentScheduleConflicts(targetClass, student, centerId, classId);

        ClassEnrollment existingTargetEnrollment = findEnrollment(targetClass.getId(), student.getId())
                .orElse(null);
        if (existingTargetEnrollment != null
                && (existingTargetEnrollment.getStatus() == EnrollmentStatus.ACTIVE
                || existingTargetEnrollment.getStatus() == EnrollmentStatus.PENDING
                || existingTargetEnrollment.getStatus() == EnrollmentStatus.SUSPENDED)) {
            throw new DuplicateResourceException("Student already has an active enrollment in the target class.");
        }
        if (existingTargetEnrollment != null
                && existingTargetEnrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
            throw new BusinessRuleException("The target enrollment has already been transferred and cannot be reused.");
        }

        // Mark old enrollment as TRANSFERRED
        oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
        oldEnrollment.setDroppedAt(Instant.now());
        classEnrollmentRepository.save(oldEnrollment);

        // A student may already have a historical DROPPED row in the target
        // class. Reuse that row; inserting another one violates the natural
        // key (class_id, student_user_id).
        ClassEnrollment newEnrollment = existingTargetEnrollment;
        if (newEnrollment != null) {
            if (newEnrollment.getStatus() == EnrollmentStatus.ACTIVE
                    || newEnrollment.getStatus() == EnrollmentStatus.PENDING
                    || newEnrollment.getStatus() == EnrollmentStatus.SUSPENDED) {
                throw new DuplicateResourceException(
                        "Học sinh đã có ghi danh đang hoạt động trong lớp đích.");
            }
            if (newEnrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                throw new BusinessRuleException(
                        "Ghi danh trong lớp đích đã được chuyển tiếp, không thể tái sử dụng.");
            }

            newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
            newEnrollment.setEnrolledByUser(currentUser);
            newEnrollment.setDropReason(null);
            newEnrollment.setDroppedAt(null);
            newEnrollment.setDroppedByUser(null);
            newEnrollment.setTransferredFromEnrollment(oldEnrollment);
        } else {
            newEnrollment = ClassEnrollment.builder()
                    .studentUser(student)
                    .clazz(targetClass)
                    .center(oldEnrollment.getCenter())
                    .enrolledByUser(currentUser)
                    .status(EnrollmentStatus.ACTIVE)
                    .transferredFromEnrollment(oldEnrollment)
                    .build();
        }
        newEnrollment = classEnrollmentRepository.save(newEnrollment);

        // Link old → new
        oldEnrollment.setTransferredToEnrollment(newEnrollment);
        classEnrollmentRepository.save(oldEnrollment);

        // Calculate fee difference
        BigDecimal feeDifference = calculateTransferFeeDifference(oldEnrollment, newEnrollment, centerId);

        // Generate fee record for new class if fee difference > 0 (student owes more)
        if (feeDifference.compareTo(BigDecimal.ZERO) > 0) {
            generateTransferFeeRecord(newEnrollment, feeDifference);
        } else if (feeDifference.compareTo(BigDecimal.ZERO) < 0) {
            // Student overpaid: create Refund REQUESTED
            autoCreateRefundRequest(oldEnrollment, feeDifference.abs(), currentUser,
                    "Chênh lệch học phí do chuyển lớp sang " + targetClass.getName());
        }

        writeAuditLog(currentUser, oldEnrollment.getCenter(), "ENROLLMENT_TRANSFER", "ClassEnrollment",
                oldEnrollment.getId(), "Chuyển từ lớp " + classId + " sang lớp " + request.getTargetClassId()
                        + (request.getNote() != null ? ", ghi chú: " + request.getNote() : ""));

        return TransferResponse.builder()
                .oldEnrollment(toResponse(oldEnrollment))
                .newEnrollment(toResponse(newEnrollment))
                .feeDifference(feeDifference)
                .build();
    }

    // ── Fee calculation helpers ───────────────────────────────────────────

    /**
     * Calculates the fee difference when a student drops.
     * Returns positive value if student overpaid (should get refund).
     */
    private BigDecimal calculateDropFeeDifference(ClassEnrollment enrollment, Long centerId) {
        Long classId = enrollment.getClazz().getId();
        Long studentUserId = enrollment.getStudentUser().getId();

        // Total paid for this class by this student (sum of all ACTIVE payments)
        BigDecimal totalPaid = paymentRepository.sumActivePaymentsByStudentAndClass(studentUserId, classId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        // Count sessions: total events for class vs events already passed
        List<ScheduleEvent> allEvents = scheduleEventRepository
                .findAllByClazz_IdAndCenter_IdOrderByEventDateAscStartTimeAsc(classId, centerId);
        long totalSessions = allEvents.stream()
                .filter(e -> e.getStatus() != ScheduleEventStatus.CANCELLED)
                .count();
        if (totalSessions == 0) return totalPaid; // no sessions = refund everything paid

        long attendedSessions = allEvents.stream()
                .filter(e -> e.getStatus() != ScheduleEventStatus.CANCELLED)
                .filter(e -> e.getEventDate().isBefore(LocalDate.now()) || e.getEventDate().isEqual(LocalDate.now()))
                .count();

        // Get total fee amount for the class
        BigDecimal totalFee = feeRecordRepository.findAllByStudentUser_IdAndClazz_Id(studentUserId, classId)
                .stream()
                .map(FeeRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Price per session
        BigDecimal pricePerSession = totalFee.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        BigDecimal valueConsumed = pricePerSession.multiply(BigDecimal.valueOf(attendedSessions));

        // Difference = totalPaid - valueConsumed (positive = overpaid → refund)
        return totalPaid.subtract(valueConsumed);
    }

    /**
     * Calculates fee difference when transferring.
     * Returns positive if student needs to pay more, negative if overpaid.
     */
    private BigDecimal calculateTransferFeeDifference(ClassEnrollment oldEnrollment,
                                                      ClassEnrollment newEnrollment,
                                                      Long centerId) {
        // Remaining value from old class
        BigDecimal dropDiff = calculateDropFeeDifference(oldEnrollment, centerId);
        BigDecimal oldClassRemainingValue = dropDiff; // positive = student has credit

        // Fee for new class (monthly fee or course fee)
        BigDecimal newClassFee = BigDecimal.ZERO;
        if (newEnrollment.getClazz().getMonthlyFee() != null) {
            newClassFee = BigDecimal.valueOf(newEnrollment.getClazz().getMonthlyFee());
        }

        // feeDifference = newClassFee - oldClassRemainingCredit
        // positive = student owes more, negative = student gets refund
        return newClassFee.subtract(oldClassRemainingValue);
    }

    private void generateTransferFeeRecord(ClassEnrollment enrollment, BigDecimal amount) {
        String currentMonth = YearMonth.now().toString();
        Optional<FeeRecord> existing = feeRecordRepository.findByStudentUser_IdAndClazz_IdAndMonth(
                enrollment.getStudentUser().getId(), enrollment.getClazz().getId(), currentMonth);

        if (existing.isPresent()) {
            FeeRecord feeRecord = existing.get();
            if (feeRecord.getStatus() == FeeStatus.CANCELLED
                    && (feeRecord.getPaidAmount() == null
                    || feeRecord.getPaidAmount().compareTo(BigDecimal.ZERO) == 0)) {
                feeRecord.setAmount(amount);
                feeRecord.setPaidAmount(BigDecimal.ZERO);
                feeRecord.setDueDate(LocalDate.now().plusDays(feeGraceDays));
                feeRecord.setStatus(FeeStatus.UNPAID);
            }
            return;
        }

        FeeRecord feeRecord = FeeRecord.builder()
                .studentUser(enrollment.getStudentUser())
                .center(enrollment.getCenter())
                .clazz(enrollment.getClazz())
                .amount(amount)
                .paidAmount(BigDecimal.ZERO)
                .month(currentMonth)
                .dueDate(LocalDate.now().plusDays(feeGraceDays))
                .status(FeeStatus.UNPAID)
                .build();
        feeRecordRepository.save(feeRecord);
    }

    /**
     * Auto-creates a Refund with status REQUESTED for fee overpayment.
     */
    private void autoCreateRefundRequest(ClassEnrollment enrollment, BigDecimal amount,
                                          User currentUser, String reason) {
        // Find the most recent ACTIVE payment for this student+class to link the refund
        Long classId = enrollment.getClazz().getId();
        Long studentUserId = enrollment.getStudentUser().getId();

        var payments = paymentRepository.findActivePaymentsByStudentAndClass(studentUserId, classId);
        if (payments.isEmpty()) return; // no payments to refund from

        var payment = payments.get(0); // most recent

        Refund refund = Refund.builder()
                .payment(payment)
                .center(enrollment.getCenter())
                .amount(amount)
                .reason(reason)
                .createdBy(currentUser)
                .requestedBy(currentUser)
                .status(RefundStatus.REQUESTED)
                .relatedEnrollment(enrollment)
                .build();
        refundRepository.save(refund);
    }

    // ── Audit log helper ─────────────────────────────────────────────────

    private void writeAuditLog(User user, Center center, String action, String entityType,
                                Long entityId, String description) {
        AuditLog logEntry = AuditLog.builder()
                .center(center)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditLogRepository.save(logEntry);
    }
}
