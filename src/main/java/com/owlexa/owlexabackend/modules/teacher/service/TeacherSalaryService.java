package com.owlexa.owlexabackend.modules.teacher.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.common.exception.TenancyViolationException;
import com.owlexa.owlexabackend.modules.teacher.dto.request.TeacherSalaryRequest;
import com.owlexa.owlexabackend.modules.teacher.dto.response.TeacherSalaryResponse;
import com.owlexa.owlexabackend.modules.teacher.entity.TeacherCenterProfile;
import com.owlexa.owlexabackend.modules.teacher.repository.TeacherCenterProfileRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
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

/**
 * Quản lý salary riêng theo từng center của TEACHER.
 *
 * Tại sao tách service riêng (không nhét vào TeacherService):
 * - Salary là dữ liệu nhạy cảm, cần tách rõ boundary bảo mật.
 * - TeacherService hiện có đã phình to với create/update/delete/bulk.
 * - Sau này có thể tách microservice hoặc cache salary riêng.
 *
 * Quy tắc nghiệp vụ (theo auth-roles-plan.md):
 * - Chỉ OWNER của center tương ứng mới được set/get salary.
 * - TEACHER có thể thuộc nhiều center, mỗi center có 1 salary riêng.
 * - salary nullable: OWNER có thể chưa set khi mới thêm teacher.
 */
@Service
@RequiredArgsConstructor
public class TeacherSalaryService {

    private static final String DEFAULT_CURRENCY = "VND";

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;
    private final TeacherCenterProfileRepository profileRepository;

    /**
     * Lấy salary hiện tại của một teacher tại center hiện tại.
     *
     * Trả về response kể cả khi salary chưa được set
     * (lúc đó salary = null trong response).
     */
    @Transactional(readOnly = true)
    public TeacherSalaryResponse get(Long teacherId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);
        assertTeacherExistsInCenter(teacherId, centerId);

        TeacherCenterProfile profile = profileRepository
                .findByTeacher_IdAndCenter_Id(teacherId, centerId)
                .orElse(null);

        return toResponse(teacherId, centerId, profile);
    }

    /**
     * Set hoặc update salary cho teacher tại center hiện tại.
     *
     * Upsert pattern:
     * - Nếu chưa có profile → tạo mới với salary + currency.
     * - Nếu đã có → update salary và currency (nếu currency được truyền).
     *
     * Tại sao upsert thay vì bắt buộc tạo trước:
     * - Giảm số bước cho UX: OWNER có thể set salary ngay sau khi add teacher.
     * - Khớp với hành vi của TeacherService.create() hiện tại:
     *   tạo teacher KHÔNG tự tạo profile salary, nên bước set salary
     *   là bước độc lập sau đó.
     */
    @Transactional
    public TeacherSalaryResponse upsert(Long teacherId, TeacherSalaryRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);
        assertTeacherExistsInCenter(teacherId, centerId);

        validateAmount(request.getSalary());

        TeacherCenterProfile profile = profileRepository
                .findByTeacher_IdAndCenter_Id(teacherId, centerId)
                .orElse(null);

        if (profile == null) {
            Center center = centerRepository.getReferenceById(centerId);
            User teacher = userRepository.getReferenceById(teacherId);

            profile = TeacherCenterProfile.builder()
                    .teacher(teacher)
                    .center(center)
                    .salary(request.getSalary())
                    .currency(request.getCurrency() != null ? request.getCurrency() : DEFAULT_CURRENCY)
                    .build();
        } else {
            profile.setSalary(request.getSalary());
            if (request.getCurrency() != null) {
                profile.setCurrency(request.getCurrency());
            }
        }

        profile = profileRepository.save(profile);
        return toResponse(teacherId, centerId, profile);
    }

    /**
     * Xóa salary (set về null) nhưng giữ lại profile row
     * để audit trail biết là teacher này từng có salary.
     *
     * Lưu ý: method này không xóa hẳn TeacherCenterProfile — chỉ clear salary.
     * Nếu OWNER muốn xóa hoàn toàn thì gọi API delete-by-id riêng (chưa cần).
     */
    @Transactional
    public TeacherSalaryResponse clear(Long teacherId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);
        assertTeacherExistsInCenter(teacherId, centerId);

        TeacherCenterProfile profile = profileRepository
                .findByTeacher_IdAndCenter_Id(teacherId, centerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Salary profile not found for teacher " + teacherId
                                + " in center " + centerId));

        profile.setSalary(null);
        profile = profileRepository.save(profile);

        return toResponse(teacherId, centerId, profile);
    }

    // ========== HELPER METHODS ==========

    /**
     * Tại sao phải check membership:
     * - Một OWNER có thể sở hữu nhiều center.
     * - OWNER này chỉ được set salary cho center mà họ là thành viên.
     * - Center của teacher cũng phải đúng với tenant hiện tại.
     *
     * Không dùng assertOwnershipByCenter vì đã có
     * assertCenterMembership ở TeacherService — pattern nhất quán.
     */
    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage teacher salary");
        }

        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    /**
     * Kiểm tra teacher có membership tại center hiện tại không.
     * Nếu không có → 2 trường hợp:
     * 1. teacherId sai / không tồn tại → ResourceNotFoundException.
     * 2. teacher tồn tại nhưng không thuộc center này → TenancyViolationException.
     *
     * Phân biệt 2 case này để debug dễ hơn và tránh lộ thông tin
     * qua thông báo lỗi chung chung.
     */
    private void assertTeacherExistsInCenter(Long teacherId, Long centerId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + teacherId));

        if (teacher.getRole() != Role.TEACHER) {
            throw new BusinessRuleException(
                    "User " + teacherId + " is not a TEACHER (role=" + teacher.getRole() + ")");
        }

        Membership membership = membershipRepository
                .findByUser_IdAndCenter_Id(teacherId, centerId)
                .orElseThrow(() -> new TenancyViolationException(
                        "Teacher " + teacherId + " is not a member of center " + centerId));
    }

    private void validateAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("salary is required");
        }
        if (amount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BadRequestException("salary must be >= 0");
        }
    }

    private TeacherSalaryResponse toResponse(Long teacherId, Long centerId, TeacherCenterProfile profile) {
        User teacher = userRepository.getReferenceById(teacherId);

        if (profile == null) {
            return TeacherSalaryResponse.builder()
                    .teacherUserId(teacherId)
                    .centerId(centerId)
                    .teacherFullName(teacher.getFullName())
                    .teacherPhoneNumber(teacher.getPhoneNumber())
                    .salary(null)
                    .currency(null)
                    .createdAt(null)
                    .updatedAt(null)
                    .build();
        }

        return TeacherSalaryResponse.builder()
                .teacherUserId(teacherId)
                .centerId(centerId)
                .teacherFullName(teacher.getFullName())
                .teacherPhoneNumber(teacher.getPhoneNumber())
                .salary(profile.getSalary())
                .currency(profile.getCurrency())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private Long requiredCurrentCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved. Ensure the user has an active membership.");
        }
        return centerId;
    }
}