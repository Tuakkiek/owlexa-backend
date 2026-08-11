package com.owlexa.owlexabackend.student.service;
import com.owlexa.owlexabackend.modules.student.service.StudentService;
import com.owlexa.owlexabackend.modules.student.dto.request.BulkStudentRequest;
import com.owlexa.owlexabackend.modules.student.dto.request.StudentRequest;
import com.owlexa.owlexabackend.modules.student.dto.response.BulkStudentError;
import com.owlexa.owlexabackend.modules.student.dto.response.BulkStudentResult;
import com.owlexa.owlexabackend.modules.student.dto.response.StudentResponse;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BulkStudentValidationException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void loginAs(String phoneNumber) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        phoneNumber,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setCurrentCenterId(Long centerId) {
        TenantContext.setCurrentTenantId(centerId);
    }

    private User user(Long id, String phoneNumber, Role role) {
        User user = new User();
        user.setId(id);
        user.setPhoneNumber(phoneNumber);
        user.setRole(role);
        user.setFullName("User " + id);
        return user;
    }

    private Center center(
            Long id,
            String name,
            String subdomain,
            User owner
    ) {
        Center center = new Center();
        center.setId(id);
        center.setName(name);
        center.setSubdomain(subdomain);
        center.setOwner(owner);
        center.setCreatedAt(Instant.now());
        return center;
    }

    private StudentRequest request(
            String fullName,
            String email,
            String phoneNumber
    ) {
        return StudentRequest.builder()
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();
    }

    private Membership membership(User user, Center center, User joinedBy) {
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setCenter(center);
        membership.setJoinedByUser(joinedBy);
        return membership;
    }

    @Test
    void create_whenStudentDoesExists_shouldCreateStudentAndMembership() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);

        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        StudentRequest request = request(
                " Nguyen Van A ",
                "student@examle.com",
                "0987654321"
        );

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(owner));

        when(membershipRepository.existsByUser_IdAndCenter_Id(
                1L,
                10L
        ))
                .thenReturn(true);

        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(center));
        when(userRepository.findByPhoneNumber("0987654321"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any()))
                .thenReturn("encoded-temporary-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User student = invocation.getArgument(0);
                    student.setId(100L);
                    return student;
                });

        when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = studentService.create(request);

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getCenterId()).isEqualTo(10L);
        assertThat(response.getTemporaryPassword()).isNotBlank();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedStudent = userCaptor.getValue();

        assertThat(savedStudent.getPhoneNumber()).isEqualTo("0987654321");
        assertThat(savedStudent.getEmail()).isEqualTo("student@examle.com");
        assertThat(savedStudent.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(savedStudent.getRole()).isEqualTo(Role.STUDENT);
        assertThat(savedStudent.getPassword()).isEqualTo("encoded-temporary-password");

        ArgumentCaptor<Membership> membershipCaptor =
                ArgumentCaptor.forClass(Membership.class);

        verify(membershipRepository).save(membershipCaptor.capture());

        Membership savedMembership = membershipCaptor.getValue();

        assertThat(savedMembership.getUser().getId()).isEqualTo(100L);
        assertThat(savedMembership.getCenter()).isEqualTo(center);
        assertThat(savedMembership.getJoinedByUser()).isEqualTo(owner);
    }

    @Test
    void create_whenStudentAlreadyExistsAndAlreadyInCenter_shouldThrowDuplicateResourceException() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        User existingStudent = user(100L, "0987654321", Role.STUDENT);
        existingStudent.setEmail("student@example.com");

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(true);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(existingStudent));
        when(membershipRepository.existsByUser_IdAndCenter_Id(100L, 10L)).thenReturn(true);

        StudentRequest request = request("Nguyen Van A", "student@example.com", "0987654321");

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(com.owlexa.owlexabackend.common.exception.DuplicateResourceException.class)
                .hasMessageContaining("Số điện thoại đã tồn tại.");

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void create_whenExistingUserIsNotStudent_shouldThrowBadRequestException() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);
        User teacher = user(100L, "0987654321", Role.TEACHER);
        Center center = center(10L, "Owlexa HCM", "owlexa-hcm", owner);

        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(true);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(teacher));

        StudentRequest request = request("Nguyen Van A", "student@example.com", "0987654321");

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không phải Học sinh");

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void create_whenCenterNotFound_shouldThrowResourceNotFoundException() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);

        when(userRepository.findByPhoneNumber("0901234567"))
                .thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L))
                .thenReturn(true);
        when(centerRepository.findById(10L))
                .thenReturn(Optional.empty());

        StudentRequest request = request("Nguyen Tuan Kiet", "student@examle.com", "0987654321");

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Center not found with id: 10");

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }


    // ─────────────────────────────────────────────────────────────────
    // bulkCreate tests
    // ─────────────────────────────────────────────────────────────────

    private BulkStudentRequest buildBulkStudentRequest(List<BulkStudentRequest.Item> items) {
        BulkStudentRequest request = new BulkStudentRequest();
        request.setStudents(items);
        return request;
    }

    private BulkStudentRequest.Item buildBulkStudentItem(String phone, String fullName, String email) {
        BulkStudentRequest.Item item = new BulkStudentRequest.Item();
        item.setPhoneNumber(phone);
        item.setFullName(fullName);
        item.setEmail(email);
        return item;
    }

    private void stubOwnerAndCenter(User owner, Center center) {
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(true);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
    }

    @Test
    @DisplayName("bulkCreate: list null → BadRequestException")
    void bulkCreate_whenListIsNull_shouldThrowBadRequest() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        BulkStudentRequest request = buildBulkStudentRequest(null);

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("bulkCreate: list rỗng → BadRequestException")
    void bulkCreate_whenListIsEmpty_shouldThrowBadRequest() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        BulkStudentRequest request = buildBulkStudentRequest(List.of());

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("bulkCreate: tất cả student mới → CREATED, có temporaryPassword")
    void bulkCreate_whenAllNewStudents_shouldCreateUsersAndMemberships() {
        loginAs("0901234567");
        setCurrentCenterId(10L);

        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);

        BulkStudentRequest.Item a = buildBulkStudentItem("0911111111", "Student A", "a@x.com");
        BulkStudentRequest.Item b = buildBulkStudentItem("0922222222", "Student B", "b@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(a, b));

        stubOwnerAndCenter(owner, center);

        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber("0922222222")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.existsByEmail("b@x.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(900L);
            return u;
        });
        when(membershipRepository.save(any(Membership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<BulkStudentResult> results = studentService.bulkCreate(request);

        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo("CREATED");
            assertThat(r.getTemporaryPassword()).isNotNull().isNotBlank();
        });
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: student đã tồn tại + đã trong center → ALREADY_IN_CENTER, không save")
    void bulkCreate_whenExistingStudentAlreadyInCenter_shouldReturnAlreadyInCenter() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        User existingStudent = user(50L, "0911111111", Role.STUDENT);

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", "a@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        stubOwnerAndCenter(owner, center);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(existingStudent));
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, 10L)).thenReturn(true);

        List<BulkStudentResult> results = studentService.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("ALREADY_IN_CENTER");
        assertThat(results.get(0).getTemporaryPassword()).isNull();
        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    @DisplayName("bulkCreate: student đã tồn tại + CHƯA trong center → ADDED_TO_CENTER, tạo membership")
    void bulkCreate_whenExistingStudentNotInCenter_shouldAddToCenter() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        User existingStudent = user(50L, "0911111111", Role.STUDENT);

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", "a@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        stubOwnerAndCenter(owner, center);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(existingStudent));
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, 10L)).thenReturn(false);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<BulkStudentResult> results = studentService.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("ADDED_TO_CENTER");
        verify(membershipRepository).save(any(Membership.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: phone trùng trong cùng request → INVALID_INPUT 'Duplicate phone number in request'")
    void bulkCreate_whenDuplicatePhoneInSameRequest_shouldAddError() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        stubOwnerAndCenter(owner, center);

        BulkStudentRequest.Item first = buildBulkStudentItem("0911111111", "Student A", null);
        BulkStudentRequest.Item second = buildBulkStudentItem("0911111111", "Student B", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(first, second));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class)
                .satisfies(ex -> {
                    List<BulkStudentError> errors = ((BulkStudentValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getRow()).isEqualTo(2);
                    assertThat(errors.get(0).getMessage()).contains("Duplicate phone number in request");
                });
    }

    @Test
    @DisplayName("bulkCreate: phone trùng + email dup cùng lúc → aggregate cả 2 errors")
    void bulkCreate_whenDuplicatePhoneAndEmailBothFail_shouldAggregateBothErrors() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        stubOwnerAndCenter(owner, center);

        BulkStudentRequest.Item dup1 = buildBulkStudentItem("0911111111", "Student A", null);
        BulkStudentRequest.Item dup2 = buildBulkStudentItem("0911111111", "Student B", null);
        BulkStudentRequest.Item emailDup = buildBulkStudentItem("0922222222", "Student C", "dup@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(dup1, dup2, emailDup));

        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class)
                .satisfies(ex -> {
                    List<BulkStudentError> errors = ((BulkStudentValidationException) ex).getErrors();
                    assertThat(errors).hasSize(2);
                    assertThat(errors).extracting(BulkStudentError::getRow).containsExactly(2, 3);
                });
    }

    @Test
    @DisplayName("bulkCreate: existing user role != STUDENT → ROLE_CONFLICT")
    void bulkCreate_whenExistingUserRoleConflict_shouldAddError() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        User teacher = user(70L, "0911111111", Role.TEACHER);

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", "a@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        stubOwnerAndCenter(owner, center);
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class)
                .satisfies(ex -> {
                    List<BulkStudentError> errors = ((BulkStudentValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getStatus()).isEqualTo("ROLE_CONFLICT");
                    assertThat(errors.get(0).getMessage()).contains("not STUDENT");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("bulkCreate: existing STUDENT phone → pass phase 1 (không phải conflict)")
    void bulkCreate_whenExistingStudentPhone_shouldPassPhase1() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        User existingStudent = user(80L, "0911111111", Role.STUDENT);

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", "a@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        stubOwnerAndCenter(owner, center);
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.findByPhoneNumber("0911111111")).thenReturn(Optional.of(existingStudent));
        when(membershipRepository.existsByUser_IdAndCenter_Id(80L, 10L)).thenReturn(true);

        List<BulkStudentResult> results = studentService.bulkCreate(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("ALREADY_IN_CENTER");
    }

    @Test
    @DisplayName("bulkCreate: phone với leading/trailing whitespace → trim trước khi check duplicate")
    void bulkCreate_whenPhoneHasWhitespace_shouldTrimBeforeDuplicateCheck() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        stubOwnerAndCenter(owner, center);

        BulkStudentRequest.Item a = buildBulkStudentItem(" 0911111111 ", "Student A", null);
        BulkStudentRequest.Item b = buildBulkStudentItem("0911111111", "Student B", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(a, b));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class)
                .satisfies(ex -> {
                    List<BulkStudentError> errors = ((BulkStudentValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getMessage()).contains("Duplicate phone number in request");
                });
    }

    @Test
    @DisplayName("bulkCreate: fullName blank → INVALID_INPUT (giống teacher)")
    void bulkCreate_whenFullNameBlank_shouldAddError() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);
        stubOwnerAndCenter(owner, center);

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "   ", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class)
                .satisfies(ex -> {
                    List<BulkStudentError> errors = ((BulkStudentValidationException) ex).getErrors();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getMessage()).contains("required");
                });
    }

    @Test
    @DisplayName("bulkCreate: validation phase 1 fail → phase 2 không save")
    void bulkCreate_whenValidationFails_phase2ShouldNotExecute() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        Center center = center(10L, "C", "c", owner);

        BulkStudentRequest.Item invalid = buildBulkStudentItem(null, "Invalid", null);
        BulkStudentRequest.Item valid = buildBulkStudentItem("0911111111", "Valid", "v@x.com");
        BulkStudentRequest request = buildBulkStudentRequest(List.of(invalid, valid));

        stubOwnerAndCenter(owner, center);
        when(userRepository.existsByEmail("v@x.com")).thenReturn(false);

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BulkStudentValidationException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    @DisplayName("bulkCreate: TenantContext null (sau khi auth OK) → BadRequestException 'Tenant context'")
    void bulkCreate_whenTenantContextIsNull_shouldThrowBadRequest() {
        loginAs("0901234567");
        User owner = user(1L, "0901234567", Role.OWNER);
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        // No setCurrentCenterId → TenantContext null

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }

    @Test
    @DisplayName("bulkCreate: caller không phải OWNER → AccessDeniedException")
    void bulkCreate_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        loginAs("teacher-x");
        setCurrentCenterId(10L);

        User nonOwner = user(99L, "teacher-x", Role.TEACHER);
        when(userRepository.findByPhoneNumber("teacher-x")).thenReturn(Optional.of(nonOwner));

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("bulkCreate: center không tồn tại → ResourceNotFoundException")
    void bulkCreate_whenCenterNotFound_shouldThrowResourceNotFound() {
        loginAs("0901234567");
        setCurrentCenterId(10L);
        User owner = user(1L, "0901234567", Role.OWNER);
        when(userRepository.findByPhoneNumber("0901234567")).thenReturn(Optional.of(owner));
        when(membershipRepository.existsByUser_IdAndCenter_Id(1L, 10L)).thenReturn(true);
        when(centerRepository.findById(10L)).thenReturn(Optional.empty());

        BulkStudentRequest.Item item = buildBulkStudentItem("0911111111", "Student A", null);
        BulkStudentRequest request = buildBulkStudentRequest(List.of(item));

        assertThatThrownBy(() -> studentService.bulkCreate(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}

