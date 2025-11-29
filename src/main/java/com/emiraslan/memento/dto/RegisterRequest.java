package com.emiraslan.memento.dto;

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
    private String firstName;

    @NotBlank(message = "LAST_NAME_REQUIRED")
    private String lastName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_FORMAT_INVALID")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_TOO_SHORT")
    private String password;

    @Pattern(regexp = "^\\d+$", message = "PHONE_NUMBER_ONLY_DIGITS")
    private String phoneNumber;

    @NotNull(message = "ROLE_REQUIRED")
    private UserRole role;
}
