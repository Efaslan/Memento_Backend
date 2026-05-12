package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.Email;
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
public class LoginRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_FORMAT_INVALID")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, max = 30, message = "PASSWORD_MUST_BE_MIN_8_MAX_30_CHARACTERS")
    private String password;

    // nullable in case of new devices
    private Integer deviceId;

    @Size(max = 100, message = "DEVICE_MODEL_TOO_LONG")
    private String deviceModel;
    @Size(max = 50, message = "OS_VERSION_TOO_LONG")
    private String osVersion;
}
