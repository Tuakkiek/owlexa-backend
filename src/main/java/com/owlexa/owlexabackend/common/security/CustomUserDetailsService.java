package com.owlexa.owlexabackend.common.security;
import com.owlexa.owlexabackend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.service.PermissionResolver;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionResolver permissionResolver;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            Set<String> permissions = permissionResolver.resolvePermissions(user.getId(), user.getRole());
            for (String perm : permissions) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        return new CustomUserDetails(
                user.getId(),
                user.getPhoneNumber(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                user.isActive(),
                authorities
        );
    }
}
