package com.owlexa.owlexabackend.common.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {

                if (jwtUtil.isRefreshToken(token)) {
                    chain.doFilter(request, response);
                    return;
                }

                String phoneNumber = jwtUtil.extractSubject(token);
                String sessionId   = jwtUtil.extractSessionId(token);

                if (phoneNumber != null && sessionId != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserSession session = sessionRepository.findByIdAndActiveTrue(sessionId).orElse(null);
                    if (session == null) {
                        chain.doFilter(request, response);
                        return;
                    }

                    UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    request.setAttribute("currentSessionId", sessionId);

                    // Resolve tenant from the session's center (set during login).
                    // Falls back to the user's first membership if session center is null.
                    if (session.getCenter() != null) {
                        TenantContext.setCurrentTenantId(session.getCenter().getId());
                    } else {
                        userRepository.findByPhoneNumber(phoneNumber)
                                .ifPresent(user -> membershipRepository
                                        .findAllByUser_Id(user.getId())
                                        .stream()
                                        .findFirst()
                                        .ifPresent(m -> TenantContext.setCurrentTenantId(m.getCenter().getId())));
                    }
                }
            } catch (Exception e) {
                // Token invalid hoặc expired — Spring Security sẽ trả 401 tự động
            }
        }

        chain.doFilter(request, response);
    }
}