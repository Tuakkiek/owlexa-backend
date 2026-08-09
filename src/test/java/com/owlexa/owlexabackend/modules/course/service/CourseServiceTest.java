package com.owlexa.owlexabackend.modules.course.service;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.entity.ClassStatus;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.course.dto.request.CourseRequest;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseDeleteValidationResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseResponse;
import com.owlexa.owlexabackend.modules.course.dto.response.CourseStatisticsResponse;
import com.owlexa.owlexabackend.modules.course.entity.Course;
import com.owlexa.owlexabackend.modules.course.repository.CourseRepository;
import com.owlexa.owlexabackend.modules.enrollment.repository.ClassEnrollmentRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CenterRepository centerRepository;

    private CourseService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;
    private static final Long COURSE_ID = 1L;

    private Center center;

    @BeforeEach
    void setUp() {
        service = new CourseService(courseRepository, classRepository, classEnrollmentRepository, userRepository, membershipRepository, centerRepository);
        com.owlexa.owlexabackend.common.context.TenantContext.setCurrentTenantId(CENTER_ID);

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);

        center = new Center();
        center.setId(CENTER_ID);
        center.setName("Test Center");

        org.mockito.Mockito.lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        org.mockito.Mockito.lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
        org.mockito.Mockito.lenient().when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
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
        course.setCenter(center);
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
        when(courseRepository.existsByCodeAndCenter_Id("VSTEP-B1", CENTER_ID)).thenReturn(false);
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
    @DisplayName("create: duplicate code in same center → DuplicateResourceException")
    void create_whenDuplicateCode_shouldThrowDuplicate() {
        when(courseRepository.existsByCodeAndCenter_Id("VSTEP-B1", CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("findAll: returns active courses for current center ordered by name")
    void findAll_shouldReturnActiveCourses() {
        when(courseRepository.findAllByCenter_IdAndIsActiveTrueOrderByNameAsc(CENTER_ID))
                .thenReturn(List.of(buildCourse(1L, "IELTS-55", "IELTS 5.5"),
                        buildCourse(2L, "VSTEP-B1", "VSTEP B1")));

        List<CourseResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("IELTS 5.5");
    }

    @Test
    @DisplayName("findById: course exists in current center → returns course")
    void findById_whenExists_shouldReturnCourse() {
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

        CourseResponse response = service.findById(COURSE_ID);

        assertThat(response.getId()).isEqualTo(COURSE_ID);
        assertThat(response.getCode()).isEqualTo("VSTEP-B1");
    }

    @Test
    @DisplayName("findById: course not found or belongs to another center → ResourceNotFoundException")
    void findById_whenNotFound_shouldThrowResourceNotFound() {
        when(courseRepository.findByIdAndCenter_Id(999L, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: valid → updates course")
    void update_whenValid_shouldUpdateCourse() {
        Course existing = buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1 Old");
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequest req = CourseRequest.builder()
                .code("VSTEP-B1")
                .name("VSTEP B1 New")
                .build();

        CourseResponse response = service.update(COURSE_ID, req);

        assertThat(response.getName()).isEqualTo("VSTEP B1 New");
    }

    @Test
    @DisplayName("update: code changed to duplicate in center → DuplicateResourceException")
    void update_whenCodeChangedToDuplicate_shouldThrowDuplicate() {
        Course existing = buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1");
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByCodeAndCenter_Id("VSTEP-B2", CENTER_ID)).thenReturn(true);

        CourseRequest req = CourseRequest.builder()
                .code("VSTEP-B2")
                .name("VSTEP B1")
                .build();

        assertThatThrownBy(() -> service.update(COURSE_ID, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("delete: course exists in center → deletes")
    void delete_whenExists_shouldDelete() {
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));
        when(classRepository.existsByCourse_Id(COURSE_ID)).thenReturn(false);

        service.delete(COURSE_ID);
    }

    @Test
    @DisplayName("delete: course not found → ResourceNotFoundException")
    void delete_whenNotFound_shouldThrowResourceNotFound() {
        when(courseRepository.findByIdAndCenter_Id(999L, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: course referenced by classes → throws BusinessRuleException COURSE_IN_USE")
    void delete_whenReferenced_shouldThrowBusinessRuleException() {
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));
        when(classRepository.existsByCourse_Id(COURSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(COURSE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể xóa khóa học này");
    }

    @Test
    @DisplayName("getStatistics: returns correct statistics")
    void getStatistics_shouldReturnCorrectStats() {
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

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
        when(courseRepository.findByIdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "VSTEP-B1", "VSTEP B1")));

        Class activeClass = Class.builder().id(100L).name("Class B1.1").status(ClassStatus.ACTIVE).build();
        when(classRepository.findAllByCourse_IdAndCenter_Id(COURSE_ID, CENTER_ID)).thenReturn(List.of(activeClass));

        CourseDeleteValidationResponse validation = service.validateDelete(COURSE_ID);

        assertThat(validation.isCanDelete()).isFalse();
        assertThat(validation.getDependencies()).hasSize(1);
        assertThat(validation.getDependencies().get(0).getClassName()).isEqualTo("Class B1.1");
    }
}
