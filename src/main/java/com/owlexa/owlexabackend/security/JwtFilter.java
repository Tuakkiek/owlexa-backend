package com.owlexa.owlexabackend.security;

import com.owlexa.owlexabackend.repository.UserSessionRepository;
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

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionRepository sessionRepository;

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

                    boolean sessionActive = sessionRepository.existsByIdAndActiveTrue(sessionId);
                    if (!sessionActive) {
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
                }
            } catch (Exception e) {
                // Token invalid hoặc expired — Spring Security sẽ trả 401 tự động
            }
        }

        chain.doFilter(request, response);
    }
}