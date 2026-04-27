package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String email;

    @NotBlank(message = "OTP_REQUIRED")
    @Pattern(regexp = "^\\d{6}$", message = "OTP_MUST_BE_6_DIGITS")
    private String otpCode;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 10, max = 30, message = "PASSWORD_MUST_BE_MIN_10_MAX_30_CHARACTERS")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).+$",
            message = "Your password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
    )
    private String newPassword;
}
