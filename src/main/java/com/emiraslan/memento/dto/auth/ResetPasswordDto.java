package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDto {

    @Email(message = "INVALID_EMAIL_FORMAT")
    @NotBlank(message = "EMAIL_REQUIRED")
    private String email;

    @NotBlank(message = "OTP_REQUIRED")
    @Pattern(regexp = "^\\d{6}$", message = "OTP_MUST_BE_6_DIGITS")
    private String otpCode;

    @NotNull(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_TOO_SHORT")
    private String newPassword;
}
