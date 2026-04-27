package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.BloodType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileRequestDto {

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
    @Pattern(regexp = "^[1-9]\\d{9}$", message = "PHONE_NUMBER_ONLY_DIGITS_DO_NOT_START_WITH_0")
    private String phoneNumber;

    @NotNull(message = "DATE_OF_BIRTH_REQUIRED")
    @Past(message = "DATE_OF_BIRTH_MUST_BE_IN_PAST")
    private LocalDate dateOfBirth;

    // Yetişkin/Yaşlı bir insan için mantıklı sınırlar (cm)
    @NotNull(message = "HEIGHT_REQUIRED")
    @Range(min = 50, max = 251, message = "HEIGHT_BETWEEN_50_AND_251_CM")
    private Integer heightCm;

    // Yetişkin/Yaşlı bir insan için mantıklı sınırlar (kg)
    @NotNull(message = "WEIGHT_REQUIRED")
    @Range(min = 30, max = 635, message = "WEIGHT_BETWEEN_30_AND_635_KG")
    private Double weightKg;

    @NotNull(message = "BLOOD_TYPE_REQUIRED")
    private BloodType bloodType;

    @Size(max = 255, message = "EMERGENCY_NOTES_TOO_LONG")
    private String emergencyNotes;
}