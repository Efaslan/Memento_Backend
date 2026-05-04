package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @NotBlank(message = "REFRESH_TOKEN_REQUIRED")
    @Size(max = 255, message = "REFRESH_TOKEN_TOO_LONG")
    private String refreshToken;
}

