package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequestDto {

    @NotBlank(message = "REFRESH_TOKEN_REQUIRED")
    @Size(max = 255, message = "REFRESH_TOKEN_TOO_LONG")
    private String refreshToken;
}
