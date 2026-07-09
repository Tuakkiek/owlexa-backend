package com.owlexa.owlexabackend.modules.document.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.class_management.entity.Class;
import com.owlexa.owlexabackend.modules.class_management.repository.ClassRepository;
import com.owlexa.owlexabackend.modules.document.dto.request.StudentDocumentRequest;
import com.owlexa.owlexabackend.modules.document.dto.response.StudentDocumentResponse;
import com.owlexa.owlexabackend.modules.document.entity.DocumentType;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import com.owlexa.owlexabackend.modules.document.repository.StudentDocumentRepository;
import com.owlexa.owlexabackend.modules.enrollment.entity.ClassEnrollment;
import com.owlexa.owlexabackend.modules.enrollment.entity.EnrollmentStatus;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDocumentServiceTest {

    @Mock private StudentDocumentRepository studentDocumentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;

    private StudentDocumentService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final String STUDENT_PHONE = "0900000002";
    private static final Long OWNER_ID = 1L;
    private static final Long STUDENT_ID = 100L;
    private static final Long CENTER_ID = 10L;
    private static final Long OTHER_CENTER_ID = 99L;
    private static final Long CLASS_ID = 50L;
    private static final Long DOC_ID = 500L;

    @BeforeEach
    void setUp() {
        service = new StudentDocumentService(
                studentDocumentRepository, classEnrollmentRepository, classRepository,
                centerRepository, userRepository, membershipRepository
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_PHONE, null, List.of())
        );

        User owner = new User();
        owner.setId(OWNER_ID);
        owner.setPhoneNumber(OWNER_PHONE);
        owner.setRole(Role.OWNER);
        lenient().when(userRepository.findByPhoneNumber(OWNER_PHONE)).thenReturn(Optional.of(owner));
        lenient().when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Center buildCenter(Long id) {
        Center center = new Center();
        center.setId(id);
        return center;
    }

    private Class buildClass(Long centerId) {
        Center center = buildCenter(centerId);
        Class clazz = new Class();
        clazz.setId(CLASS_ID);
        clazz.setName("Class A");
        clazz.setCenter(center);
        return clazz;
    }

    private StudentDocument buildDocument(Long id, String title) {
        StudentDocument doc = new StudentDocument();
        doc.setId(id);
        doc.setTitle(title);
        doc.setDocumentType(DocumentType.PDF);
        doc.setFileUrl("https://example.com/file-" + id + ".pdf");
        doc.setCenter(buildCenter(CENTER_ID));
        doc.setClazz(buildClass(CENTER_ID));
        return doc;
    }

    private StudentDocumentRequest buildRequest() {
        StudentDocumentRequest req = new StudentDocumentRequest();
        req.setTitle("Lesson 1 Notes");
        req.setType(DocumentType.PDF);
        req.setUrl("https://example.com/lesson1.pdf");
        return req;
    }

    @Test
    @DisplayName("createForClass: OWNER + hợp lệ → tạo document")
    void createForClass_whenValid_shouldCreateDocument() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(CENTER_ID)));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter(CENTER_ID)));
        when(studentDocumentRepository.save(any(StudentDocument.class))).thenAnswer(invocation -> {
            StudentDocument d = invocation.getArgument(0);
            d.setId(DOC_ID);
            return d;
        });

        StudentDocumentResponse response = service.createForClass(CLASS_ID, buildRequest());

        assertThat(response.getId()).isEqualTo(DOC_ID);
        assertThat(response.getTitle()).isEqualTo("Lesson 1 Notes");
        assertThat(response.getUrl()).isEqualTo("https://example.com/lesson1.pdf");
        assertThat(response.getClassName()).isEqualTo("Class A");
    }

    @Test
    @DisplayName("createForClass: class thuộc center khác → TenancyViolationException")
    void createForClass_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(OTHER_CENTER_ID)));

        assertThatThrownBy(() -> service.createForClass(CLASS_ID, buildRequest()))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("createForClass: title trống → BadRequestException")
    void createForClass_whenTitleBlank_shouldThrowBadRequest() {
        StudentDocumentRequest req = buildRequest();
        req.setTitle("");

        assertThatThrownBy(() -> service.createForClass(CLASS_ID, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createForClass: url trống → BadRequestException")
    void createForClass_whenUrlBlank_shouldThrowBadRequest() {
        StudentDocumentRequest req = buildRequest();
        req.setUrl("");

        assertThatThrownBy(() -> service.createForClass(CLASS_ID, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("createForClass: caller không phải OWNER → AccessDeniedException")
    void createForClass_whenCallerIsTeacher_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.createForClass(CLASS_ID, buildRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("findMyDocuments: STUDENT ACTIVE → trả về documents từ các class enrolled")
    void findMyDocuments_whenStudentActive_shouldReturnDocuments() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(STUDENT_PHONE, null, List.of())
        );
        User student = new User();
        student.setId(STUDENT_ID);
        student.setPhoneNumber(STUDENT_PHONE);
        student.setRole(Role.STUDENT);
        when(userRepository.findByPhoneNumber(STUDENT_PHONE)).thenReturn(Optional.of(student));
        when(membershipRepository.existsByUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID)).thenReturn(true);

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setClazz(buildClass(CENTER_ID));
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        when(classEnrollmentRepository.findAllByStudentUser_IdAndCenter_Id(STUDENT_ID, CENTER_ID))
                .thenReturn(List.of(enrollment));
        when(studentDocumentRepository.findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildDocument(1L, "Doc A"), buildDocument(2L, "Doc B")));

        List<StudentDocumentResponse> response = service.findMyDocuments();

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("findClassDocuments: OWNER + class hợp lệ → trả về documents")
    void findClassDocuments_whenValid_shouldReturnDocuments() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(CENTER_ID)));
        when(studentDocumentRepository.findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(CLASS_ID, CENTER_ID))
                .thenReturn(List.of(buildDocument(1L, "Doc A")));

        List<StudentDocumentResponse> response = service.findClassDocuments(CLASS_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Doc A");
    }

    @Test
    @DisplayName("findClassDocuments: class thuộc center khác → TenancyViolationException")
    void findClassDocuments_whenClassInOtherCenter_shouldThrowTenancyViolation() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(buildClass(OTHER_CENTER_ID)));

        assertThatThrownBy(() -> service.findClassDocuments(CLASS_ID))
                .isInstanceOf(TenancyViolationException.class);
    }

    @Test
    @DisplayName("findClassDocuments: class không tồn tại → ResourceNotFoundException")
    void findClassDocuments_whenClassNotFound_shouldThrowResourceNotFound() {
        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findClassDocuments(CLASS_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createForClass: TenantContext null → BadRequestException")
    void createForClass_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.createForClass(CLASS_ID, buildRequest()))
                .isInstanceOf(BadRequestException.class);
    }
}