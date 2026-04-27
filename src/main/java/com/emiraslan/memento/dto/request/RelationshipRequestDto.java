package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.RelationshipType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipRequestDto {

    @NotBlank(message = "TARGET_EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String targetEmail;

    @NotNull(message = "RELATIONSHIP_TYPE_REQUIRED")
    private RelationshipType relationshipType;

    @NotNull(message = "IS_PRIMARY_CONTACT_REQUIRED")
    private Boolean isPrimaryContact;

    // nullable in case for doctors
    @Pattern(regexp = "^\\d{6}$", message = "OTP_MUST_BE_6_DIGITS")
    private String otpCode;
}