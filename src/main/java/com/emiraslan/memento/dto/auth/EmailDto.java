package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {

    @Email(message = "INVALID_EMAIL_FORMAT")
    @NotBlank(message = "EMAIL_REQUIRED")
    private String email;
}
