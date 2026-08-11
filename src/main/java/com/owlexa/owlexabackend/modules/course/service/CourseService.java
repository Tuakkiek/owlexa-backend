package com.owlexa.owlexabackend.modules.course.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.course.dto.request.CourseRequest;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseClassResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseDependencyDto;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseStatisticsResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;

    @Transactional
    public CourseResponse create(CourseRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        if (courseRepository.existsByCodeAndCenter_Id(request.getCode().trim(), centerId)) {
            throw new DuplicateResourceException("Course code already exists: " + request.getCode());
        }

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        Course course = Course.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .description(request.getDescription())
                .defaultDuration(request.getDefaultDuration())
                .defaultSessionCount(request.getDefaultSessionCount())
                .defaultMonthlyFee(request.getDefaultMonthlyFee())
                .defaultTeacherUserId(request.getDefaultTeacherUserId())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .center(center)
                .build();

        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        Long centerId = requiredCurrentCenterId();
        return courseRepository.findAllByCenter_IdAndIsActiveTrueOrderByNameAsc(centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAllIncludingInactive() {
        Long centerId = requiredCurrentCenterId();
        return courseRepository.findAllByCenter_IdOrderByNameAsc(centerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        Long centerId = requiredCurrentCenterId();
        Course course = courseRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return toResponse(course);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Course course = courseRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (!course.getCode().equalsIgnoreCase(request.getCode().trim())
                && courseRepository.existsByCodeAndCenter_Id(request.getCode().trim(), centerId)) {
            throw new DuplicateResourceException("Course code already exists: " + request.getCode());
        }

        course.setCode(request.getCode().trim());
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        course.setDefaultDuration(request.getDefaultDuration());
        course.setDefaultSessionCount(request.getDefaultSessionCount());
        course.setDefaultMonthlyFee(request.getDefaultMonthlyFee());
        course.setDefaultTeacherUserId(request.getDefaultTeacherUserId());
        if (request.getIsActive() != null) {
            course.setIsActive(request.getIsActive());
        }

        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Course course = courseRepository.findByIdAndCenter_Id(id, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + id));

        if (classRepository.existsByCourse_Id(id)) {
            throw new BusinessRuleException("COURSE_IN_USE", "Không thể xóa khóa học này vì đang có các lớp học liên kết với nó.");
        }

        courseRepository.delete(course);
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        return centerId;
    }

    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    @Transactional(readOnly = true)
    public CourseStatisticsResponse getStatistics(Long courseId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        courseRepository.findByIdAndCenter_Id(courseId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Class> classes = classRepository.findAllByCourse_IdAndCenter_Id(courseId, centerId);

        long activeClasses = classes.stream().filter(c -> c.getStatus() == ClassStatus.ACTIVE).count();
        long finishedClasses = classes.stream().filter(c -> c.getStatus() == ClassStatus.FINISHED).count();
        long plannedClasses = classes.stream().filter(c -> c.getStatus() == ClassStatus.PLANNED).count();

        long totalEnrolled = 0;
        for (Class c : classes) {
            totalEnrolled += classEnrollmentRepository.countByClazz_IdAndStatus(c.getId(), EnrollmentStatus.ACTIVE);
        }

        return CourseStatisticsResponse.builder()
                .totalClasses(classes.size())
                .totalEnrolledStudents(totalEnrolled)
                .activeClasses(activeClasses)
                .finishedClasses(finishedClasses)
                .plannedClasses(plannedClasses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseClassResponse> getClasses(Long courseId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        courseRepository.findByIdAndCenter_Id(courseId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Class> classes = classRepository.findAllByCourse_IdAndCenter_Id(courseId, centerId);

        return classes.stream()
                .map(c -> {
                    List<String> teachers = c.getSchedules() != null ?
                            c.getSchedules().stream()
                                    .map(s -> s.getTeacherUser() != null ? s.getTeacherUser().getFullName() : null)
                                    .filter(java.util.Objects::nonNull)
                                    .distinct()
                                    .toList() : List.of();

                    long studentCount = classEnrollmentRepository.countByClazz_IdAndStatus(c.getId(), EnrollmentStatus.ACTIVE);

                    return CourseClassResponse.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .status(c.getStatus())
                            .teachers(teachers)
                            .studentCount(studentCount)
                            .scheduleCount(c.getSchedules() != null ? (long) c.getSchedules().size() : 0L)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDeleteValidationResponse validateDelete(Long courseId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertCenterMembership(currentUser, centerId);

        Course course = courseRepository.findByIdAndCenter_Id(courseId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Class> classes = classRepository.findAllByCourse_IdAndCenter_Id(courseId, centerId);
        boolean canDelete = classes.isEmpty();

        List<CourseDependencyDto> dependencies = classes.stream()
                .map(c -> {
                    List<String> teachers = c.getSchedules() != null ?
                            c.getSchedules().stream()
                                    .map(s -> s.getTeacherUser() != null ? s.getTeacherUser().getFullName() : null)
                                    .filter(java.util.Objects::nonNull)
                                    .distinct()
                                    .toList() : List.of();

                    long studentCount = classEnrollmentRepository.countByClazz_IdAndStatus(c.getId(), EnrollmentStatus.ACTIVE);

                    return CourseDependencyDto.builder()
                            .className(c.getName())
                            .status(c.getStatus().name())
                            .teacherNames(String.join(", ", teachers))
                            .studentCount((int) studentCount)
                            .build();
                })
                .toList();

        String message = canDelete ? "Course can be deleted." : "This course cannot be deleted because there are classes associated with it.";

        return CourseDeleteValidationResponse.builder()
                .canDelete(canDelete)
                .message(message)
                .dependencies(dependencies)
                .build();
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .defaultDuration(course.getDefaultDuration())
                .defaultSessionCount(course.getDefaultSessionCount())
                .defaultMonthlyFee(course.getDefaultMonthlyFee())
                .defaultTeacherUserId(course.getDefaultTeacherUserId())
                .isActive(course.getIsActive())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
