package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.ClassRequest;
import com.owlexa.owlexabackend.dto.response.ClassResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Class;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.ClassRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.ScheduleRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;

    // Create
    @Transactional
    public ClassResponse create(ClassRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        if (classRepository.existsByNameAndCenterId(request.getName().trim(), centerId)) {
            throw new DuplicateResourceException("Class name is already exists in this center");
        }

        Class newClass = Class.builder()
                .name(request.getName().trim())
                .vstepLevel(request.getVstepLevel())
                .maxStudents(request.getMaxStudent())
                .monthlyFee(request.getMonthlyFee())
                .isActive(true)
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

        List<Class> classes = classRepository.findAllByCenterId(centerId);

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
                .findAllByTeacherUserIdAndCenterId(currentUser.getId(), centerId)
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
    // Update
    @Transactional
    public ClassResponse update(Long classId, ClassRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Class existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        if(!existingClass.getCenter().getId().equals(centerId)) {
            throw new AccessDeniedException("You do not have permission to edit this class");
        }

        String newName = request.getName();
        if(!existingClass.getName().equalsIgnoreCase(request.getName())
            && classRepository.existsByNameAndCenterId(newName, centerId)) {
            throw new DuplicateResourceException("Class name is already exists in this center");
        }

        existingClass.setName(newName);
        existingClass.setVstepLevel(request.getVstepLevel());
        existingClass.setMaxStudents(request.getMaxStudent());
        existingClass.setMonthlyFee(request.getMonthlyFee());

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
            throw new AccessDeniedException("You do not have permission to delete this class");
        }

        classRepository.delete(existingClass);
    }

    // Helper function

    // To response
    private ClassResponse toResponse(Class clazz) {
        return ClassResponse.builder()
                .id(clazz.getId())
                .name(clazz.getName())
                .vstepLevel(clazz.getVstepLevel())
                .maxStudents(clazz.getMaxStudents())
                .monthFee(clazz.getMonthlyFee())
                .isActive(clazz.getIsActive())
                .centerId(clazz.getCenter().getId())
                .build();
    }
    // Get current user
    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
    // Required current center ID
    private Long requiredCurrentCenterId() {
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
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
        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

}
