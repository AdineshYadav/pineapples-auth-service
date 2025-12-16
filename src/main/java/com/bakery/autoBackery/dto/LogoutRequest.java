package com.bakery.autoBackery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;

    @NotBlank(message = "Username must not be blank")
    private String username;
}

