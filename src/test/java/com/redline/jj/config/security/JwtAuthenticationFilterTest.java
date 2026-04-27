package com.redline.jj.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.ApiResponse;
import com.redline.jj.domain.user.User;
import com.redline.jj.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({JwtAuthenticationFilterTest.TestController.class, JwtUtil.class, CustomUserDetailsService.class})
@TestPropertySource(properties = {
    "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktMjU2LWJpdHM=",
    "jwt.access-token-expiration=1800000",
    "jwt.refresh-token-expiration=604800000"
})
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http,
                                            JwtUtil jwtUtil,
                                            CustomUserDetailsService userDetailsService,
                                            ObjectMapper objectMapper) throws Exception {
            JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/test/public").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    (req, res, e) -> {
                        res.setContentType("application/json;charset=UTF-8");
                        res.setStatus(401);
                        res.getWriter().write(
                            objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED))
                        );
                    }
                ))
                .build();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test/public")
        String pub() { return "public"; }

        @GetMapping("/api/test/private")
        String priv() { return "private"; }
    }

    @BeforeEach
    void setUp() {
        User mockUser = User.builder()
            .userId("u1")
            .userPw("pw")
            .userName("테스터")
            .build();
        given(userRepository.findByUserId("u1")).willReturn(Optional.of(mockUser));
    }

    @Test
    void 유효한_Bearer_토큰_보호_경로_200() throws Exception {
        String token = jwtUtil.generateAccessToken("u1");

        mockMvc.perform(get("/api/test/private")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void 토큰_없음_공개_경로_200() throws Exception {
        mockMvc.perform(get("/api/test/public"))
            .andExpect(status().isOk());
    }

    @Test
    void 토큰_없음_보호_경로_401() throws Exception {
        mockMvc.perform(get("/api/test/private"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료_토큰_보호_경로_401() throws Exception {
        JwtUtil shortLivedUtil = new JwtUtil(
            "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktMjU2LWJpdHM=", -1L, -1L
        );
        String expiredToken = shortLivedUtil.generateAccessToken("u1");

        mockMvc.perform(get("/api/test/private")
                .header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 위조_토큰_보호_경로_401() throws Exception {
        JwtUtil otherUtil = new JwtUtil(
            "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1vbmx5LTI1Ni1iaXRz", 1800000L, 604800000L
        );
        String forgedToken = otherUtil.generateAccessToken("u1");

        mockMvc.perform(get("/api/test/private")
                .header("Authorization", "Bearer " + forgedToken))
            .andExpect(status().isUnauthorized());
    }
}
