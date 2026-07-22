package com.owlexa.owlexabackend.modules.class_management.service;
import com.owlexa.owlexabackend.modules.class_management.dto.request.ClassRequest;
import com.owlexa.owlexabackend.modules.class_management.dto.response.ClassResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import com.owlexa.owlexabackend.modules.student.dto.response.StudentResponse;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherClassStudentsResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final CourseRepository courseRepository;

    // Create
    @Transactional
    public ClassResponse create(ClassRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        if (classRepository.existsByNameAndCenter_Id(request.getName().trim(), centerId)) {
            throw new DuplicateResourceException("Class name is already exists in this center");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        Integer maxStudents = request.getMaxStudent() != null ? request.getMaxStudent() : course.getDefaultMaxStudents();
        Double monthlyFee = request.getMonthlyFee() != null ? request.getMonthlyFee() : course.getDefaultMonthlyFee();

        Class newClass = Class.builder()
                .name(request.getName().trim())
                .course(course)
                .status(ClassStatus.PLANNED)
                .maxStudents(maxStudents)
                .monthlyFee(monthlyFee)
                .center(center)
                .build();

        newClass = classRepository.save(newClass);
        return toResponse(newClass);
    }
    // Find all
    @Transactional(readOnly = true)
    public List<ClassResponse> findAll() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertCenterMembership(currentUser, centerId);

        List<Class> classes = classRepository.findAllByCenter_Id(centerId);

        List<ClassResponse> result = new ArrayList<>();

        for (Class c : classes) {
            result.add(toResponse(c));
        }
        return result;
    }

    // Find my classes as Teacher
    @Transactional(readOnly = true)
    public List<ClassResponse> findMyClassesAsTeacher() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new org.springframework.security.access.AccessDeniedException("Only TEACHER can access their own classes");
        }

        List<Long> classIds = scheduleRepository
                .findAllByTeacherUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .map(s -> s.getClazz().getId())
                .distinct()
                .toList();

        return classIds.stream()
                .map(id -> classRepository.findById(id).orElse(null))
                .filter(c -> c != null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherClassStudentsResponse> findMyClassesWithStudentsAsTeacher() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can access their own classes");
        }

        List<Long> classIds = scheduleRepository
                .findAllByTeacherUser_IdAndCenter_Id(currentUser.getId(), centerId)
                .stream()
                .map(schedule -> schedule.getClazz().getId())
                .distinct()
                .toList();

        return classIds.stream()
                .map(classId -> {
                    Class clazz = classRepository.findById(classId)
                            .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

                    List<StudentResponse> students = classEnrollmentRepository
                            .findAllByClazz_IdAndStatus(classId, EnrollmentStatus.ACTIVE)
                            .stream()
                            .map(ClassEnrollment::getStudentUser)
                            .map(student -> StudentResponse.builder()
                                    .userId(student.getId())
                                    .phoneNumber(student.getPhoneNumber())
                                    .fullName(student.getFullName())
                                    .centerId(centerId)
                                    .temporaryPassword(null)
                                    .build())
                            .toList();

                    return TeacherClassStudentsResponse.builder()
                            .id(clazz.getId())
                            .className(clazz.getName())
                            .studentCount((long) students.size())
                            .students(students)
                            .build();
                })
                .toList();
    }

    // Find all classes with students — for OWNER attendance overview
    @Transactional(readOnly = true)
    public List<TeacherClassStudentsResponse> findAllClassesWithStudentsForOwner() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can access all classes");
        }

        List<Class> allClasses = classRepository.findAllByCenter_Id(centerId);

        return allClasses.stream()
                .filter(clazz -> clazz.getStatus() == ClassStatus.ACTIVE)
                .map(clazz -> {
                    List<StudentResponse> students = classEnrollmentRepository
                            .findAllByClazz_IdAndStatus(clazz.getId(), EnrollmentStatus.ACTIVE)
                            .stream()
                            .map(ClassEnrollment::getStudentUser)
                            .map(student -> StudentResponse.builder()
                                    .userId(student.getId())
                                    .phoneNumber(student.getPhoneNumber())
                                    .fullName(student.getFullName())
                                    .centerId(centerId)
                                    .temporaryPassword(null)
                                    .build())
                            .toList();

                    return TeacherClassStudentsResponse.builder()
                            .id(clazz.getId())
                            .className(clazz.getName())
                            .studentCount((long) students.size())
                            .students(students)
                            .build();
                })
                .toList();
    }
    // Update
    @Transactional
    public ClassResponse update(Long classId, ClassRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if(!existingClass.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        String newName = request.getName();
        if(!existingClass.getName().equalsIgnoreCase(request.getName())
            && classRepository.existsByNameAndCenter_Id(newName, centerId)) {
            throw new DuplicateResourceException("Class name is already exists in this center");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));
        existingClass.setCourse(course);

        existingClass.setName(newName);
        Integer maxStudents = request.getMaxStudent() != null ? request.getMaxStudent() : course.getDefaultMaxStudents();
        Double monthlyFee = request.getMonthlyFee() != null ? request.getMonthlyFee() : course.getDefaultMonthlyFee();
        existingClass.setMaxStudents(maxStudents);
        existingClass.setMonthlyFee(monthlyFee);

        existingClass = classRepository.save(existingClass);

        return toResponse(existingClass);
    }

    // Delete
    @Transactional
    public void delete(Long classId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        if(!existingClass.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        classRepository.delete(existingClass);
    }

    // Helper function

    // To response
    private ClassResponse toResponse(Class clazz) {
        return ClassResponse.builder()
                .id(clazz.getId())
                .name(clazz.getName())
                .maxStudents(clazz.getMaxStudents())
                .monthFee(clazz.getMonthlyFee())
                .status(clazz.getStatus())
                .isActive(clazz.getIsActive())
                .centerId(clazz.getCenter().getId())
                .courseId(clazz.getCourse() != null ? clazz.getCourse().getId() : null)
                .courseName(clazz.getCourse() != null ? clazz.getCourse().getName() : null)
                .courseCode(clazz.getCourse() != null ? clazz.getCourse().getCode() : null)
                .build();
    }

    // ── Lifecycle: Update Status (any status → any status) ────────────

    @Transactional
    public ClassResponse updateStatus(Long classId, ClassStatus newStatus) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        assertOwnerAndCenterMembership(currentUser, centerId);

        Class clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if (!clazz.getCenter().getId().equals(centerId)) {
            throw new TenancyViolationException("Class " + classId + " belongs to another center");
        }

        clazz.setStatus(newStatus);
        clazz = classRepository.save(clazz);
        return toResponse(clazz);
    }
    // Get current user
    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
    // Required current center ID
    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }
    // Assert Owner and Center Membership
    private void assertOwnerAndCenterMembership(User current, Long centerId) {
        if (current.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage classes");
        }
        assertCenterMembership(current, centerId);
    }

    // Asser Center Membership
    private void assertCenterMembership(User currentUser, Long centerId) {
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

}
