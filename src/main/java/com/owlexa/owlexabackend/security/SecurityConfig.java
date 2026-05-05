package com.owlexa.owlexabackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/admin/centers/**")
                        .hasAnyAuthority("OWNER", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/admin/centers/**")
                        .hasAnyAuthority("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/admin/centers/**")
                        .hasAnyAuthority("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/admin/centers/**")
                        .hasAnyAuthority("OWNER")

                        .requestMatchers(HttpMethod.GET, "/admin/teachers/**")
                        .hasAnyAuthority("OWNER", "TEACHER")


                        .requestMatchers(HttpMethod.POST, "/admin/teachers/**")
                        .hasAnyAuthority("OWNER")


                        .requestMatchers("/admin/**")
                        .hasAnyAuthority("OWNER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
