package com.owlexa.owlexabackend.modules.course.service;

import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.course.dto.request.CourseRequest;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseStatisticsResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseClassResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseDeleteValidationResponse;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private CourseService service;


    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long COURSE_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new CourseService(courseRepository, classRepository, classEnrollmentRepository, userRepository, membershipRepository);
        com.owlexa.owlexabackend.common.context.TenantContext.setCurrentTenantId(CENTER_ID);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        com.owlexa.owlexabackend.modules.user.entity.User owner = new com.owlexa.owlexabackend.modules.user.entity.User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(com.owlexa.owlexabackend.modules.user.entity.Role.OWNER);
        org.mockito.Mockito.lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        org.mockito.Mockito.lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        com.owlexa.owlexabackend.common.context.TenantContext.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }


    private Course buildCourse(Long id, String code, String name) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setName(name);
        course.setIsActive(true);
        return course;
    }

    private CourseRequest buildCreateRequest() {
        return CourseRequest.builder()
                .code("VSTEP-B1")
                .name("VSTEP B1")
                .defaultMonthlyFee(1500000.0)
                .build();
    }

    @Test
    @DisplayName("create: valid request → creates course")
    void create_whenValid_shouldCreateCourse() {
        when(courseRepository.existsByCode("VSTEP-B1")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(COURSE_ID);
            return c;
        });

        CourseResponse response = service.create(buildCreateRequest());

        assertThat(response.getId()).isEqualTo(COURSE_ID);
        assertThat(response.getCode()).isEqualTo("VSTEP-B1");
        assertThat(response.getName()).isEqualTo("VSTEP B1");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("create: duplicate code → DuplicateResourceException")
    void create_whenDuplicateCode_shouldThrowDuplicate() {
        when(courseRepository.existsByCode("VSTEP-B1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("findAll: returns active courses ordered by name")
    void findAll_shouldReturnActiveCourses() {
        when(courseRepository.findAllByIsActiveTrueOrderByNameAsc())
                .thenReturn(List.of(buildCourse(1L, "IELTS-55", "IELTS 5.5"),
                        buildCourse(2L, "VSTEP-B1", "VSTEP B1")));

        List<CourseResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("IELTS 5.5");
    }

    @Test
    @DisplayName("findById: course exists → returns course")
    void findById_whenExists_shouldReturnCourse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

        CourseResponse response = service.findById(COURSE_ID);

        assertThat(response.getId()).isEqualTo(COURSE_ID);
        assertThat(response.getCode()).isEqualTo("VSTEP-B1");
    }

    @Test
    @DisplayName("findById: course not found → ResourceNotFoundException")
    void findById_whenNotFound_shouldThrowResourceNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: valid → updates course")
    void update_whenValid_shouldUpdateCourse() {
        Course existing = buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1 Old");
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequest req = CourseRequest.builder()
                .code("VSTEP-B1")
                .name("VSTEP B1 New")
                .build();

        CourseResponse response = service.update(COURSE_ID, req);

        assertThat(response.getName()).isEqualTo("VSTEP B1 New");
    }

    @Test
    @DisplayName("update: code changed to duplicate → DuplicateResourceException")
    void update_whenCodeChangedToDuplicate_shouldThrowDuplicate() {
        Course existing = buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1");
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByCode("VSTEP-B2")).thenReturn(true);

        CourseRequest req = CourseRequest.builder()
                .code("VSTEP-B2")
                .name("VSTEP B1")
                .build();

        assertThatThrownBy(() -> service.update(COURSE_ID, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("delete: course exists → deletes")
    void delete_whenExists_shouldDelete() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));
        when(classRepository.existsByCourse_Id(COURSE_ID)).thenReturn(false);

        service.delete(COURSE_ID);
    }

    @Test
    @DisplayName("delete: course not found → ResourceNotFoundException")
    void delete_whenNotFound_shouldThrowResourceNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: course referenced by classes → throws BusinessRuleException COURSE_IN_USE")
    void delete_whenReferenced_shouldThrowBusinessRuleException() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));
        when(classRepository.existsByCourse_Id(COURSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(COURSE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể xóa khóa học này");
    }

    @Test
    @DisplayName("getStatistics: returns correct statistics")
    void getStatistics_shouldReturnCorrectStats() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

        Class activeClass = Class.builder().id(100L).status(ClassStatus.ACTIVE).build();
        Class plannedClass = Class.builder().id(101L).status(ClassStatus.PLANNED).build();
        when(classRepository.findAllByCourse_IdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(List.of(activeClass, plannedClass));
        when(classEnrollmentRepository.countByClazz_IdAndStatus(100L, com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE)).thenReturn(5L);
        when(classEnrollmentRepository.countByClazz_IdAndStatus(101L, com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus.ACTIVE)).thenReturn(0L);

        CourseStatisticsResponse stats = service.getStatistics(COURSE_ID);

        assertThat(stats.getTotalClasses()).isEqualTo(2);
        assertThat(stats.getActiveClasses()).isEqualTo(1);
        assertThat(stats.getPlannedClasses()).isEqualTo(1);
        assertThat(stats.getTotalEnrolledStudents()).isEqualTo(5);
    }

    @Test
    @DisplayName("validateDelete: returns false if classes exist")
    void validateDelete_whenReferenced_shouldReturnFalse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

        Class activeClass = Class.builder().id(100L).name("Class B1.1").status(ClassStatus.ACTIVE).build();
        when(classRepository.findAllByCourse_IdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(List.of(activeClass));

        CourseDeleteValidationResponse validation = service.validateDelete(COURSE_ID);

        assertThat(validation.isCanDelete()).isFalse();
        assertThat(validation.getDependencies()).hasSize(1);
        assertThat(validation.getDependencies().get(0).getClassName()).isEqualTo("Class B1.1");
    }
}

