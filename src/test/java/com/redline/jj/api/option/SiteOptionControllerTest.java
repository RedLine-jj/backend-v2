package com.redline.jj.api.option;

import com.redline.jj.api.option.dto.SiteOptionLogResponse;
import com.redline.jj.api.option.dto.SiteOptionResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SiteOptionController.class)
@Import(SiteOptionControllerTest.TestSecurityConfig.class)
class SiteOptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteOptionService siteOptionService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    @Test
    void getSiteOptions_필터파라미터_정상바인딩() throws Exception {
        given(siteOptionService.listSiteOptions(eq(1L), eq(2L), eq(true)))
            .willReturn(List.of());

        mockMvc.perform(get("/api/site-options")
                .param("siteId", "1")
                .param("modelId", "2")
                .param("inStock", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSiteOptions_필터없으면_null전달() throws Exception {
        given(siteOptionService.listSiteOptions(isNull(), isNull(), isNull()))
            .willReturn(List.of());

        mockMvc.perform(get("/api/site-options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSiteOption_없는id_404_M002() throws Exception {
        given(siteOptionService.getSiteOption(999L))
            .willThrow(new BusinessException(ErrorCode.SITE_OPTION_NOT_FOUND));

        mockMvc.perform(get("/api/site-options/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("M002"));
    }

    @Test
    void getLogs_정상반환() throws Exception {
        given(siteOptionService.getLogs(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/site-options/1/logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }
}
