package com.redline.jj.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(OPTIONS, "/**").permitAll()
                .requestMatchers(POST, "/api/auth/**").permitAll()
                .requestMatchers(GET, "/api/brands", "/api/sites").permitAll()
                .requestMatchers(GET, "/api/models/**", "/api/site-options/**").permitAll()
                .requestMatchers(GET, "/api/subscriptions/top").permitAll()
                .requestMatchers(GET, "/api/dashboard/**", "/api/restocks/recent").permitAll()
                .requestMatchers(POST, "/api/batch/crawl/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (request, response, e) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED))
                    );
                }
            ))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
