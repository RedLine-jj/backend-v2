package com.redline.jj.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank
    @Size(min = 4, max = 20)
    private String userId;

    @NotBlank
    @Size(min = 8, max = 50)
    private String password;

    @NotBlank
    @Size(max = 20)
    private String userName;
}
