package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashierRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.CashierResponse;import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.service.UserPermissionService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashierServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private UserPermissionService userPermissionService;

    private CashierService service;

    private static final String OWNER_PHONE = "0900000001";
    private static final String CASHIER_PHONE = "0900000002";
    private static final Long OWNER_ID = 1L;
    private static final Long CENTER_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new CashierService(userRepository, passwordEncoder, membershipRepository, centerRepository, userPermissionService);
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

    private CashierRequest buildRequest() {
        return CashierRequest.builder()
                .phoneNumber(CASHIER_PHONE)
                .email("cashier@example.com")
                .fullName("Nguyen Thi Cashier")
                .build();
    }

    @Test
    @DisplayName("create: cashier mới → tạo User + Membership, trả password tạm")
    void create_whenNewCashier_shouldCreateUserAndMembership() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.findByPhoneNumber(CASHIER_PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        CashierResponse response = service.create(buildRequest());

        assertThat(response.getPhoneNumber()).isEqualTo(CASHIER_PHONE);
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(response.getTemporaryPassword()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("create: cashier đã tồn tại + đã trong center → không tạo mới, password = null")
    void create_whenExistingCashierInCenter_shouldNotCreateNewMembership() {
        User existingCashier = new User();
        existingCashier.setId(50L);
        existingCashier.setPhoneNumber(CASHIER_PHONE);
        existingCashier.setRole(Role.CASHIER);

        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.findByPhoneNumber(CASHIER_PHONE)).thenReturn(Optional.of(existingCashier));
        when(membershipRepository.existsByUser_IdAndCenter_Id(50L, CENTER_ID)).thenReturn(true);

        CashierResponse response = service.create(buildRequest());

        assertThat(response.getTemporaryPassword()).isNull();
    }

    @Test
    @DisplayName("create: user tồn tại nhưng role không phải CASHIER → BadRequestException")
    void create_whenExistingUserIsNotCashier_shouldThrowBadRequest() {
        User existingStudent = new User();
        existingStudent.setId(50L);
        existingStudent.setPhoneNumber(CASHIER_PHONE);
        existingStudent.setRole(Role.STUDENT);

        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.findByPhoneNumber(CASHIER_PHONE)).thenReturn(Optional.of(existingStudent));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not CASHIER");
    }

    @Test
    @DisplayName("create: email đã tồn tại → DuplicateResourceException")
    void create_whenEmailAlreadyExists_shouldThrowDuplicate() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));
        when(userRepository.existsByEmail("cashier@example.com")).thenReturn(true);

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
                new UsernamePasswordAuthenticationToken("non-owner", null, List.of())
        );
        User nonOwner = new User();
        nonOwner.setId(2L);
        nonOwner.setPhoneNumber("non-owner");
        nonOwner.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("non-owner")).thenReturn(Optional.of(nonOwner));
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(buildCenter()));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");
    }

    @Test
    @DisplayName("findAll: trả về danh sách cashier trong center")
    void findAll_shouldReturnCashiersInCenter() {
        Membership m1 = new Membership();
        User c1 = new User();
        c1.setId(50L);
        c1.setFullName("Cashier A");
        c1.setPhoneNumber("0900000050");
        m1.setUser(c1);

        when(membershipRepository.findAllByCenter_IdAndUserRole(CENTER_ID, Role.CASHIER))
                .thenReturn(List.of(m1));

        List<CashierResponse> response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFullName()).isEqualTo("Cashier A");
        assertThat(response.get(0).getTemporaryPassword()).isNull();
    }

    @Test
    @DisplayName("delete: cashier thuộc center → xóa membership")
    void delete_whenCashierInCenter_shouldDeleteMembership() {
        Membership m = new Membership();
        m.setId(123L);
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(m));

        service.delete(50L);

        org.mockito.Mockito.verify(membershipRepository).delete(m);
    }

    @Test
    @DisplayName("delete: cashier không thuộc center → ResourceNotFoundException")
    void delete_whenCashierNotInCenter_shouldThrowResourceNotFound() {
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("TenantContext null → BadRequestException")
    void create_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }

    // ─────────────────────────────────────────────────────────────────
    // update tests
    // ─────────────────────────────────────────────────────────────────

    private CashierRequest buildUpdateRequest(String phone, String email, String fullName) {
        return CashierRequest.builder()
                .phoneNumber(phone)
                .email(email)
                .fullName(fullName)
                .build();
    }

    private User buildCashier(Long id, String phone, String email, String fullName) {
        User cashier = new User();
        cashier.setId(id);
        cashier.setPhoneNumber(phone);
        cashier.setEmail(email);
        cashier.setRole(Role.CASHIER);
        cashier.setFullName(fullName);
        return cashier;
    }

    private Membership buildCashierMembership(Long cashierId, String phone, String email, String fullName) {
        Membership m = new Membership();
        m.setId(700L);
        m.setUser(buildCashier(cashierId, phone, email, fullName));
        m.setCenter(buildCenter());
        return m;
    }

    @Test
    @DisplayName("update: happy path - đổi phone + email + name → save cashier")
    void update_whenAllFieldsChanged_shouldUpdateAndSave() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashierResponse response = service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name"));

        assertThat(response.getPhoneNumber()).isEqualTo("0999999999");
        assertThat(response.getFullName()).isEqualTo("New Name");
        assertThat(response.getTemporaryPassword()).isNull();
        assertThat(membership.getUser().getPhoneNumber()).isEqualTo("0999999999");
        assertThat(membership.getUser().getEmail()).isEqualTo("new@x.com"); // normalized lowercase
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("update: cashier không thuộc center → ResourceNotFoundException")
    void update_whenCashierNotInCenter_shouldThrowResourceNotFound() {
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cashier not found in this center");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: phone thuộc user khác (không phải cashier hiện tại) → DuplicateResourceException 'Phone number already exists'")
    void update_whenPhoneBelongsToAnotherUser_shouldThrowDuplicatePhone() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(true);

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Phone number already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: email mới (khác email hiện tại) đã tồn tại ở user khác → DuplicateResourceException 'Email already exists'")
    void update_whenEmailBelongsToAnotherUser_shouldThrowDuplicateEmail() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        // Dùng phone khác để KHÔNG trigger OR-match ở line 123
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
        when(userRepository.existsByEmail("new@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: email mới giống email hiện tại (line 123 OR không trigger nếu cả 3 trùng - document behavior)")
    void update_whenEmailUnchanged_shouldSkipEmailDuplicateCheck() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        // Đổi phone và email đều mới, name đổi → KHÔNG trigger OR check ở line 123
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(50L, buildUpdateRequest("0999999999", "new@x.com", "New Name"));

        verify(userRepository).existsByEmail("new@x.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("update: phone thay đổi nhưng phone mới unique → save cashier mới")
    void update_whenPhoneChangedToUnique_shouldSave() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashierResponse response = service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name"));

        assertThat(response.getPhoneNumber()).isEqualTo("0999999999");
        assertThat(membership.getUser().getFullName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("update: phone thay đổi nhưng phone mới = phone cũ (sau trim) → logic OR phát hiện trùng → DuplicateResourceException")
    void update_whenPhoneUnchanged_shouldBeConsideredDuplicateByOrCondition() {
        // Source code line 123: cashier.getPhoneNumber().equals(phoneNumber) || ...
        // Nếu phone giữ nguyên (đã trim) → trigger throw
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0900000002", "new@x.com", "New Name")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("duplicate of the current information");

        // Tech debt: OR check causes false-positive "no-change-only-email" updates to fail
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: chỉ đổi fullName (phone + email giữ) → OR check vẫn throw duplicate (technical debt)")
    void update_whenOnlyFullNameChanged_shouldStillThrowDuplicateDueToOrCondition() {
        // Chỉ thay đổi fullName, phone và email giữ nguyên
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0900000002", "old@x.com", "New Name Only")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("duplicate of the current information");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: phone trống, email mới, name mới → save cashier với phone='' (line 123 OR không vì phone khác)")
    void update_whenRequestPhoneBlank_shouldProcessAndThrowOrMatch() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.existsByPhoneNumber("")).thenReturn(false);
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashierResponse response = service.update(50L,
                buildUpdateRequest("", "new@x.com", "New Name"));

        assertThat(response.getPhoneNumber()).isEqualTo("");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("update: email null/blank → normalizeOptionalEmail trả null → skip email duplicate check")
    void update_whenEmailNullOrBlank_shouldNormalizeToNullAndSkipEmailCheck() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // phone mới (different), email blank " " → normalizeOptionalEmail → null
        service.update(50L, buildUpdateRequest("0999999999", " ", "New Name"));

        // source code line 131: if (email != null && ...) → email==null → skip whole block
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("update: caller không phải OWNER → AccessDeniedException trước cả khi tìm membership")
    void update_whenCallerIsNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("non-owner", null, List.of())
        );
        User teacher = new User();
        teacher.setId(2L);
        teacher.setPhoneNumber("non-owner");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByPhoneNumber("non-owner")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only OWNER");

        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never())
                .findByUser_IdAndCenter_IdAndUserRole(any(), any(), any());
    }

    @Test
    @DisplayName("update: owner không thuộc center → AccessDeniedException")
    void update_whenOwnerNotMember_shouldThrowAccessDenied() {
        org.mockito.Mockito.reset(membershipRepository);
        when(membershipRepository.existsByUser_IdAndCenter_Id(OWNER_ID, CENTER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a member");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("update: TenantContext null → BadRequestException 'Tenant context'")
    void update_whenTenantContextIsNull_shouldThrowBadRequest() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tenant context");
    }

    @Test
    @DisplayName("update: response không bao gồm password (update trả null password)")
    void update_shouldReturnResponseWithoutTemporaryPassword() {
        Membership membership = buildCashierMembership(50L, "0900000002", "old@x.com", "Old Name");
        when(membershipRepository.findByUser_IdAndCenter_IdAndUserRole(50L, CENTER_ID, Role.CASHIER))
                .thenReturn(Optional.of(membership));
        when(userRepository.existsByPhoneNumber("0999999999")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashierResponse response = service.update(50L,
                buildUpdateRequest("0999999999", "new@x.com", "New Name"));

        assertThat(response.getTemporaryPassword()).isNull();
        assertThat(response.getUserId()).isEqualTo(50L);
        assertThat(response.getCenterId()).isEqualTo(CENTER_ID);
    }
}