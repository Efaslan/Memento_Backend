package com.emiraslan.memento.dto.request;

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
public class DoctorProfileRequestDto {

    @NotBlank(message = "FIRST_NAME_REQUIRED")
    @Size(max = 50, message = "FIRST_NAME_TOO_LONG")
    private String firstName;

    @NotBlank(message = "LAST_NAME_REQUIRED")
    @Size(max = 50, message = "LAST_NAME_TOO_LONG")
    private String lastName;

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String email;

    @NotBlank(message = "PHONE_NUMBER_REQUIRED")
    @Pattern(regexp = "^[1-9]\\d{9}$", message = "PHONE_NUMBER_ONLY_DIGITS_DO_NOT_START_WITH_0") // starting with 1-9, then 9 more digits
    private String phoneNumber;

    @NotBlank(message = "SPECIALIZATION_REQUIRED")
    @Size(max = 100, message = "SPECIALIZATION_TOO_LONG")
    private String specialization;

    @NotBlank(message = "HOSPITAL_NAME_REQUIRED")
    @Size(max = 100, message = "HOSPITAL_NAME_TOO_LONG")
    private String hospitalName;

    @NotBlank(message = "TITLE_REQUIRED")
    @Size(max = 50, message = "TITLE_TOO_LONG")
    private String title;
}