package com.owlexa.owlexabackend.modules.teacher.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BulkTeacherValidationException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.teacher.dto.request.BulkTeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.request.TeacherRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.response.BulkTeacherError;
import com.owlexa.owlexabackend.modules.teacher.dto.response.BulkTeacherResult;
import com.owlexa.owlexabackend.modules.teacher.entity.BulkTeacherStatus;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private TeacherService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final String TEACHER_PHONE = "0900000002";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new TeacherService(userRepository, membershipRepository, centerRepository, passwordEncoder);
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
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Center buildCenter() {
        Center center = new Center();
        center.setId(CENTER_ID);
        return center;
    }

    private TeacherRequest buildRequest() {
        return TeacherRequest.builder()
                .phoneNumber(TEACHER_PHONE)
                .email("teacher@example.com")
                .fullName("Nguyen Van Teacher")
                .build();
    }

    @Test
    @DisplayName("create: teacher mới (chưa tồn tại) → tạo User + Membership, trả về password tạm")
    void create_whenNewTeacher_shouldCreateUserAndMembership() {
        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        TeacherResponse response = service.create(buildRequest());

        assertThat(response.getPhoneNumber()).isEqualTo(TEACHER_PHONE);
        assertThat(response.getFullName()).isEqualTo("Nguyen Van Teacher");
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(response.getTemporaryPassword()).isNotNull().isNotBlank();
        assertThat(response.getTemporaryPassword().length()).isBetween(8, 10);
    }

    @Test
    @DisplayName("create: teacher đã tồn tại + đã trong center → không tạo membership mới, password = null")
    void create_whenExistingTeacherInCenter_shouldNotCreateNewMembership() {
        User existingTeacher = new User();
        existingTeacher.setId(50L);
        existingTeacher.setPhoneNumber(TEACHER_PHONE);
        existingTeacher.setRole(Role.TEACHER);

        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(existingTeacher));
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, CENTER_ID)).thenReturn(true);

        TeacherResponse response = service.create(buildRequest());

        assertThat(response.getTemporaryPassword()).isNull();
        assertThat(response.getPhoneNumber()).isEqualTo(TEACHER_PHONE);
    }

    @Test
    @DisplayName("create: user tồn tại nhưng role không phải TEACHER → BadRequestException")
    void create_whenExistingUserIsNotTeacher_shouldThrowBadRequest() {
        User existingStudent = new User();
        existingStudent.setId(50L);
        existingStudent.setPhoneNumber(TEACHER_PHONE);
        existingStudent.setRole(Role.STUDENT);

        when(userRepository.findByPhoneNumber(TEACHER_PHONE)).thenReturn(Optional.of(existingStudent));
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not TEACHER");
    }

    @Test
    @DisplayName("create: email đã tồn tại → DuplicateResourceException")
    void create_whenEmailAlreadyExists_shouldThrowDuplicate() {
        when(userRepository.existsByEmail("teacher@example.com")).thenReturn(true);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create: center không tồn tại → ResourceNotFoundException")
    void create_whenCenterNotFound_shouldThrowResourceNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: caller không phải OWNER → AccessDeniedException")
    void create_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-other", null, List.of())
        );
        User nonOwner = new User();
        nonOwner.setId(2L);
        nonOwner.setPhoneNumber("teacher-other");
        nonOwner.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-other")).thenReturn(Optional.of(nonOwner));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("findAll: trả về danh sách teacher trong center hiện tại")
    void findAll_shouldReturnTeachersInCurrentCenter() {
        Membership m1 = new Membership();
        User t1 = new User();
        t1.setId(50L);
        t1.setFullName("Teacher A");
        t1.setPhoneNumber("0900000050");
        m1.setUser(t1);

        Membership m2 = new Membership();
        User t2 = new User();
        t2.setId(60L);
        t2.setFullName("Teacher B");
        t2.setPhoneNumber("0900000060");
        m2.setUser(t2);

        when(membershipRepository.findAllByCenter_IdAndUserRole(CENTER_ID, Role.TEACHER))
                .thenReturn(List.of(m1, m2));

        List<TeacherResponse> response = service.findAll();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getFullName()).isEqualTo("Teacher A");
        assertThat(response.get(1).getFullName()).isEqualTo("Teacher B");
        assertThat(response).allSatisfy(r -> assertThat(r.getTemporaryPassword()).isNull());
    }

    @Test
    @DisplayName("update: teacher thuộc center khác → ResourceNotFoundException (cross-tenant safe)")
    void update_whenTeacherNotInCenter_shouldThrowResourceNotFound() {
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(99L, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: teacher hợp lệ + đổi phone trùng user khác → DuplicateResourceException")
    void update_whenNewPhoneBelongsToAnotherUser_shouldThrowDuplicate() {
        User teacher = new User();
        teacher.setId(50L);
        teacher.setPhoneNumber(TEACHER_PHONE);
        teacher.setEmail("teacher@example.com");
        teacher.setFullName("Old Name");

        Membership m = new Membership();
        m.setUser(teacher);

        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(m));

        TeacherRequest request = TeacherRequest.builder()
                .phoneNumber("0900000999")
                .email("teacher@example.com")
                .fullName("New Name")
                .build();
        when(userRepository.existsByPhoneNumber("0900000999")).thenReturn(true);

        assertThatThrownBy(() -> service.update(50L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("delete: teacher thuộc center → xóa membership")
    void delete_whenTeacherInCenter_shouldDeleteMembership() {
        Membership m = new Membership();
        m.setId(123L);
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.of(m));

        service.delete(50L);

        org.mockito.Mockito.verify(membershipRepository).delete(m);
    }

    @Test
    @DisplayName("delete: teacher không thuộc center → ResourceNotFoundException")
    void delete_whenTeacherNotInCenter_shouldThrowResourceNotFound() {
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.TEACHER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: TenantContext null → BadRequestException")
    void create_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("X-Tenant-ID");
    }

    // ─────────────────────────────────────────────────────────────────
    // bulkCreate tests
    // ─────────────────────────────────────────────────────────────────

    private BulkTeacherRequest buildBulkRequest(List<BulkTeacherRequest.Item> items) {
        BulkTeacherRequest req = new BulkTeacherRequest();
        req.setTeachers(items);
        return req;
    }

    private BulkTeacherRequest.Item buildBulkItem(String phone, String fullName, String email) {
        BulkTeacherRequest.Item item = new BulkTeacherRequest.Item();
        item.setPhoneNumber(phone);
        item.setFullName(fullName);
        item.setEmail(email);
        return item;
    }

    @Test
    @DisplayName("bulkCreate: list null → BadRequestException")
    void bulkCreate_whenListIsNull_shouldThrowBadRequest() {
        BulkTeacherRequest request = buildBulkRequest(null);

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("bulkCreate: list rỗng → BadRequestException")
    void bulkCreate_whenListIsEmpty_shouldThrowBadRequest() {
        BulkTeacherRequest request = buildBulkRequest(List.of());

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("bulkCreate: teacher mới (chưa tồn tại) → CREATED + có temporaryPassword")
    void bulkCreate_whenAllNewTeachers_shouldCreateUsersAndMemberships() {
        BulkTeacherRequest.Item a = buildBulkItem("0911111111", "Teacher A", "a@x.com");
        BulkTeacherRequest.Item b = buildBulkItem("0922222222", "Teacher B", "b@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(a, b));

        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("0922222222")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.existsByEmail("b@x.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(900L);
            return u;
        });

        List<BulkTeacherResult> results = service.bulkCreate(request);

        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(BulkTeacherStatus.CREATED);
            assertThat(r.getTemporaryPassword()).isNotNull().isNotBlank();
            assertThat(r.getTemporaryPassword().length()).isBetween(8, 10);
        });
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: teacher đã tồn tại + đã trong center → ALREADY_IN_CENTER, không tạo membership mới")
    void bulkCreate_whenExistingTeacherAlreadyInCenter_shouldReturnAlreadyInCenter() {
        User existing = new User();
        existing.setId(50L);
        existing.setPhoneNumber("0911111111");
        existing.setRole(Role.TEACHER);

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", "a@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, CENTER_ID)).thenReturn(true);

        List<BulkTeacherResult> results = service.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BulkTeacherStatus.ALREADY_IN_CENTER);
        assertThat(results.get(0).getTemporaryPassword()).isNull();
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    @DisplayName("bulkCreate: teacher đã tồn tại + CHƯA trong center → ADDED_TO_CENTER, tạo membership")
    void bulkCreate_whenExistingTeacherNotInCenter_shouldAddToCenter() {
        User existing = new User();
        existing.setId(50L);
        existing.setPhoneNumber("0911111111");
        existing.setRole(Role.TEACHER);

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", "a@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, CENTER_ID)).thenReturn(false);

        List<BulkTeacherResult> results = service.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BulkTeacherStatus.ADDED_TO_CENTER);
        assertThat(results.get(0).getTemporaryPassword()).isNull();
    }

    @Test
    @DisplayName("bulkCreate: 1 row thiếu phoneNumber → BulkTeacherValidationException với row=1, KHÔNG insert row nào")
    void bulkCreate_whenAnyRowMissingPhone_shouldThrowValidationAndRollbackAll() {
        BulkTeacherRequest.Item invalid = buildBulkItem(null, "No Phone", null);
        BulkTeacherRequest.Item valid = buildBulkItem("0911111111", "Valid", "v@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(invalid, valid));

        when(userRepository.existsByEmail("v@x.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class)
                .satisfies(ex -> {
                    BulkTeacherValidationException bex = (BulkTeacherValidationException) ex;
                    List<BulkTeacherError> errors = bex.getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getRow()).isEqualTo(1);
                    assertThat(errors.get(0).getStatus()).isEqualTo(BulkTeacherStatus.INVALID_INPUT);
                    assertThat(errors.get(0).getMessage()).contains("Phone number and full name are required");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: row có fullName blank → INVALID_INPUT với row index đúng")
    void bulkCreate_whenAnyRowHasBlankFullName_shouldAggregateErrors() {
        BulkTeacherRequest.Item invalid = buildBulkItem("0933333333", "   ", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(invalid));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class)
                .satisfies(ex -> {
                    List<BulkTeacherError> errors = ((BulkTeacherValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getRow()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("bulkCreate: email đã tồn tại trên user khác → INVALID_INPUT 'Email already exists'")
    void bulkCreate_whenEmailAlreadyExists_shouldAddError() {
        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", "dup@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class)
                .satisfies(ex -> {
                    List<BulkTeacherError> errors = ((BulkTeacherValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getStatus()).isEqualTo(BulkTeacherStatus.INVALID_INPUT);
                    assertThat(errors.get(0).getMessage()).contains("Email already exists");
                });
    }

    @Test
    @DisplayName("bulkCreate: email null/blank → KHÔNG check duplicate email (skip)")
    void bulkCreate_whenEmailIsNullOrBlank_shouldNotCheckDuplicate() {
        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.empty());
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(900L);
            return u;
        });

        List<BulkTeacherResult> results = service.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BulkTeacherStatus.CREATED);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("bulkCreate: phone đã tồn tại + role != TEACHER → INVALID_INPUT 'Existing User role is not TEACHER'")
    void bulkCreate_whenExistingUserRoleConflict_shouldAddError() {
        User student = new User();
        student.setId(70L);
        student.setPhoneNumber("0911111111");
        student.setRole(Role.STUDENT);

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", "a@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(student));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class)
                .satisfies(ex -> {
                    List<BulkTeacherError> errors = ((BulkTeacherValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getStatus()).isEqualTo(BulkTeacherStatus.INVALID_INPUT);
                    assertThat(errors.get(0).getMessage()).contains("Existing User role is not TEACHER");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: phone đã tồn tại + role = TEACHER → pass validation phase 1")
    void bulkCreate_whenExistingUserIsTeacher_shouldPassValidationPhase1() {
        User teacher = new User();
        teacher.setId(80L);
        teacher.setPhoneNumber("0911111111");
        teacher.setRole(Role.TEACHER);

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", "a@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(teacher));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(membershipRepository.existsByUser_IdAndCenter_Id(80L, CENTER_ID)).thenReturn(true);

        List<BulkTeacherResult> results = service.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(BulkTeacherStatus.ALREADY_IN_CENTER);
    }

    @Test
    @DisplayName("bulkCreate: nhiều row lỗi → throw exception chứa TẤT CẢ errors (atomic)")
    void bulkCreate_whenMultipleRowsInvalid_shouldAggregateAllErrors() {
        List<BulkTeacherRequest.Item> items = new ArrayList<>();
        items.add(buildBulkItem(null, "Missing Phone", null));
        items.add(buildBulkItem("0922222222", null, null));
        items.add(buildBulkItem("0933333333", "Teacher C", "dup@x.com"));
        BulkTeacherRequest request = buildBulkRequest(items);

        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class)
                .satisfies(ex -> {
                    List<BulkTeacherError> errors = ((BulkTeacherValidationException) ex).getErrors();
                    assertThat(errors).hasSize(3);
                    assertThat(errors).extracting(BulkTeacherError::getRow)
                            .containsExactly(1, 2, 3);
                });
    }

    @Test
    @DisplayName("bulkCreate: validation phase 1 fail → phase 2 KHÔNG execute (không gọi save User lần 2)")
    void bulkCreate_whenValidationFails_phase2ShouldNotExecute() {
        BulkTeacherRequest.Item invalid = buildBulkItem(null, "X", null);
        BulkTeacherRequest.Item valid = buildBulkItem("0944444444", "Valid", "v@x.com");
        BulkTeacherRequest request = buildBulkRequest(List.of(invalid, valid));

        when(userRepository.existsByEmail("v@x.com")).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BulkTeacherValidationException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    @DisplayName("bulkCreate: center không tồn tại → ResourceNotFoundException")
    void bulkCreate_whenCenterNotFound_shouldThrowResourceNotFound() {
        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("bulkCreate: caller không phải OWNER → AccessDeniedException")
    void bulkCreate_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("teacher-other", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("teacher-other");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-other")).thenReturn(Optional.of(teacher));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("bulkCreate: TenantContext null → BadRequestException")
    void bulkCreate_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("X-Tenant-ID");
    }

    @Test
    @DisplayName("bulkCreate: owner không thuộc center → AccessDeniedException")
    void bulkCreate_whenOwnerNotMemberOfCenter_shouldThrowAccessDenied() {
        // Override setUp's lenient() stub for membership check
        org.mockito.Mockito.reset(membershipRepository);
        when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(false);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        BulkTeacherRequest.Item item = buildBulkItem("0911111111", "Teacher A", null);
        BulkTeacherRequest request = buildBulkRequest(List.of(item));

        assertThatThrownBy(() -> service.bulkCreate(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a member");
    }
}