package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.RelationshipType;
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
public class RelationshipInitiationDto {
    @NotBlank(message = "RELATIVE_FIRST_NAME_REQUIRED")
    private String relativeFirstName;
    @NotBlank(message = "RELATIVE_LAST_NAME_REQUIRED")
    private String relativeLastName;
    @NotBlank(message = "RELATIVE_EMAIL_IS_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    private String relativeEmail;
    @NotBlank(message = "RELATIVE_PHONE_NUMBER_IS_REQUIRED")
    private String phoneNumber;
    private RelationshipType relationshipType;
    private Boolean isPrimaryContact;
}
