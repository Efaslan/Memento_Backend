package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.RelationshipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipInitiationDto {

    @NotBlank(message = "TARGET_EMAIL_IS_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    private String targetEmail;

    private RelationshipType relationshipType;
    private Boolean isPrimaryContact;

    // nullable for doctors
    @Pattern(regexp = "^\\d{6}$", message = "OTP_MUST_BE_6_DIGITS")
    private String otpCode;
}