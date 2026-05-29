package com.owlexa.owlexabackend.service;

import com.owlexa.owlexabackend.dto.request.CashierRequest;
import com.owlexa.owlexabackend.dto.response.CashierResponse;
import com.owlexa.owlexabackend.entity.Center;
import com.owlexa.owlexabackend.entity.Membership;
import com.owlexa.owlexabackend.entity.Role;
import com.owlexa.owlexabackend.entity.User;
import com.owlexa.owlexabackend.exception.BadRequestException;
import com.owlexa.owlexabackend.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.filter.TenantFilter;
import com.owlexa.owlexabackend.repository.CenterRepository;
import com.owlexa.owlexabackend.repository.MembershipRepository;
import com.owlexa.owlexabackend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashierService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MembershipRepository membershipRepository;
    private final CenterRepository centerRepository;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Create
    @Transactional
    public CashierResponse create(CashierRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with center id: " + centerId));

        assertOwnerAndCenterMembership(currentUser, centerId);

        // Check duplicate email
        String email = normalizeOptionalEmail(request.getEmail());

        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User cashierUser;
        String temporaryPassword = null;

        // Check if the user already exists
        var existingUser = userRepository.findByPhoneNumber(request.getPhoneNumber());
        if (existingUser.isPresent()) {
            cashierUser = existingUser.get();

            // Check role
            if (cashierUser.getRole() != Role.CASHIER) {
                throw new BadRequestException("User is not CASHIER");
            }
            // Check if the user belongs to that center.
            boolean existsMembership = membershipRepository.existsByUserIdAndCenterId(cashierUser.getId(), centerId);
            // If the cashier it not already in the center, add the cashier in the center
            if (!existsMembership) {
                createMembership(cashierUser, center, currentUser);
            }
        } else {
            temporaryPassword = generateTemporaryPassword();

            cashierUser = new User();
            cashierUser.setPhoneNumber(request.getPhoneNumber());
            cashierUser.setEmail(normalizeOptionalEmail(request.getEmail()));
            cashierUser.setFullName(request.getFullName());
            cashierUser.setRole(Role.CASHIER);
            cashierUser.setPassword(passwordEncoder.encode(temporaryPassword));
            cashierUser = userRepository.save(cashierUser);

            createMembership(cashierUser, center, currentUser);
        }

        return toResponse(cashierUser, centerId, temporaryPassword);

    }

    // Find All
    @Transactional(readOnly = true)
    public List<CashierResponse> findAll() {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        return membershipRepository.findAllByCenterIdAndUserRole(centerId, Role.CASHIER)
                .stream()
                .map(Membership::getUser)
                .map(user -> toResponse(user, centerId, null))
                .toList();
    }

    // Update
    @Transactional
    public CashierResponse update(Long cashierId, CashierRequest request) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(cashierId, centerId, Role.CASHIER)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier not found in this center"));

        User cashier = membership.getUser();

        String phoneNumber = request.getPhoneNumber().trim();
        String email = normalizeOptionalEmail(request.getEmail());
        String fullName = request.getFullName();

        if (cashier.getPhoneNumber().equals(phoneNumber) || cashier.getEmail().equals(email) || cashier.getFullName().equals(fullName)) {
            throw new DuplicateResourceException("The updated information is a duplicate of the current information");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already exists");
        }

        if (email != null && !cashier.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        cashier.setPhoneNumber(phoneNumber);
        cashier.setEmail(email);
        cashier.setFullName(fullName);

        cashier = userRepository.save(cashier);

        return toResponse(cashier, centerId, null);
    }

    // Delete
    @Transactional
    public void delete(Long cashierId) {
        User currentUser = getCurrentUser();
        Long centerId = requiredCurrentCenterId();

        assertOwnerAndCenterMembership(currentUser, centerId);

        Membership membership = membershipRepository
                .findByUserIdAndCenterIdAndUserRole(cashierId, centerId, Role.CASHIER)
                        .orElseThrow(() -> new ResourceNotFoundException("Cashier not found in this center"));

        membershipRepository.delete(membership);
    }

    // HELPER
    // Get current USER
    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    // required current USER
    private Long requiredCurrentCenterId() {
        Long centerId = TenantFilter.getCurrentCenterId();
        if (centerId == null) {
            throw new BadRequestException("Missing X-Tenant-ID header");
        }
        return centerId;
    }

    // Assert Owner and Center membership
    private void assertOwnerAndCenterMembership(User currentUser, Long centerId) {
        if (currentUser.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only OWNER can manage cashier");
        }

        boolean hasMembership = membershipRepository.existsByUserIdAndCenterId(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    // Create membership
    private void createMembership(User cashierUser, Center center, User joinedByUser) {
        Membership membership = new Membership();

        membership.setUser(cashierUser);
        membership.setCenter(center);
        membership.setJoinedByUser(joinedByUser);

        membershipRepository.save(membership);
    }

    // Generate temporary password
    private String generateTemporaryPassword() {
        int length = 8 + SECURE_RANDOM.nextInt(3);
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }

    // To response
    private CashierResponse toResponse(User user, Long centerId, String temporaryPassword) {
        return CashierResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .centerId(centerId)
                .temporaryPassword(temporaryPassword)
                .build();
    }
    // NormalizeOptionalEmail

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }

        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

}
