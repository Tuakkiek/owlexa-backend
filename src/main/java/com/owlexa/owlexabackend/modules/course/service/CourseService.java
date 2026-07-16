package com.owlexa.owlexabackend.modules.course.service;

import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.course.dto.request.CourseRequest;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse create(CourseRequest request) {
        if (courseRepository.existsByCode(request.getCode().trim())) {
            throw new DuplicateResourceException("Course code already exists: " + request.getCode());
        }

        Course course = Course.builder()
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .level(request.getLevel())
                .description(request.getDescription())
                .defaultDuration(request.getDefaultDuration())
                .defaultMonthlyFee(request.getDefaultMonthlyFee())
                .defaultMaxStudents(request.getDefaultMaxStudents())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        return courseRepository.findAllByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAllIncludingInactive() {
        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return toResponse(course);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (!course.getCode().equalsIgnoreCase(request.getCode().trim())
                && courseRepository.existsByCode(request.getCode().trim())) {
            throw new DuplicateResourceException("Course code already exists: " + request.getCode());
        }

        course.setCode(request.getCode().trim());
        course.setName(request.getName().trim());
        course.setLevel(request.getLevel());
        course.setDescription(request.getDescription());
        course.setDefaultDuration(request.getDefaultDuration());
        course.setDefaultMonthlyFee(request.getDefaultMonthlyFee());
        course.setDefaultMaxStudents(request.getDefaultMaxStudents());
        if (request.getIsActive() != null) {
            course.setIsActive(request.getIsActive());
        }

        course = courseRepository.save(course);
        return toResponse(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        courseRepository.delete(course);
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .level(course.getLevel())
                .description(course.getDescription())
                .defaultDuration(course.getDefaultDuration())
                .defaultMonthlyFee(course.getDefaultMonthlyFee())
                .defaultMaxStudents(course.getDefaultMaxStudents())
                .isActive(course.getIsActive())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
