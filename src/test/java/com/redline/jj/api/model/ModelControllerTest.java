package com.redline.jj.api.model;

import com.redline.jj.api.model.dto.ModelDetailResponse;
import com.redline.jj.api.model.dto.ModelResponse;
import com.redline.jj.common.exception.BusinessException;
import com.redline.jj.common.exception.ErrorCode;
import com.redline.jj.common.response.CursorPage;
import com.redline.jj.domain.model.Model.ModelType;
import org.junit.jupiter.api.DisplayName;
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

@WebMvcTest(ModelController.class)
@Import(ModelControllerTest.TestSecurityConfig.class)
class ModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelService modelService;

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
    @DisplayName("파라미터 바인딩이 정상 동작한다")
    void getModels_파라미터_바인딩_정상() throws Exception {
        CursorPage<ModelResponse> response = CursorPage.of(List.of(), null, false);
        given(modelService.listModels(eq(List.of(1L, 2L)), eq(List.of(ModelType.DENIM_PANTS)), eq(10L), eq(5)))
            .willReturn(response);

        mockMvc.perform(get("/api/models")
                .param("brandIds", "1", "2")
                .param("types", "DENIM_PANTS")
                .param("cursor", "10")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("size 기본값은 20이다")
    void getModels_기본값_size20() throws Exception {
        CursorPage<ModelResponse> response = CursorPage.of(List.of(), null, false);
        given(modelService.listModels(isNull(), isNull(), isNull(), eq(20))).willReturn(response);

        mockMvc.perform(get("/api/models"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 id 조회 시 404와 M001 코드를 반환한다")
    void getModel_없는id_404_M001() throws Exception {
        given(modelService.getModel(999L))
            .willThrow(new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        mockMvc.perform(get("/api/models/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("M001"));
    }

    @Test
    @DisplayName("모델 수 조회가 정상 반환된다")
    void getCount_정상반환() throws Exception {
        given(modelService.countModels()).willReturn(42L);

        mockMvc.perform(get("/api/models/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(42));
    }
}
