package com.emiraslan.memento.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_FORMAT_INVALID")
    private String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;
}
