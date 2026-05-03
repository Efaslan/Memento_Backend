package com.emiraslan.memento.dto.auth;

import com.emiraslan.memento.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "FIRST_NAME_REQUIRED")
    @Size(max = 50, message = "FIRST_NAME_TOO_LONG")
    private String firstName;

    @NotBlank(message = "LAST_NAME_REQUIRED")
    @Size(max = 50, message = "FIRST_NAME_TOO_LONG")
    private String lastName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_FORMAT_INVALID")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 10, max = 30, message = "PASSWORD_MUST_BE_MIN_10_MAX_30_CHARACTERS")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).+$",
            message = "Your password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
    )
    private String password;

    @NotBlank(message = "PHONE_NUMBER_REQUIRED")
    @Pattern(regexp = "^[1-9]\\d{9}$", message = "PHONE_NUMBER_10_DIGITS_DO_NOT_START_WITH_0")
    private String phoneNumber;

    @NotNull(message = "ROLE_REQUIRED")
    private UserRole role;
}
