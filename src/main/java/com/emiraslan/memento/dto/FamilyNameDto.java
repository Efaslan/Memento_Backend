package com.emiraslan.memento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyNameDto {
    @NotBlank(message = "FAMILY_NAME_REQUIRED")
    private String familyName;
}
