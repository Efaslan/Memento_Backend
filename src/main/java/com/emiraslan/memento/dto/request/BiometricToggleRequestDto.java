package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricToggleRequestDto {
    @NotNull(message = "BIOMETRIC_STATUS_REQUIRED")
    private Boolean isBiometricEnabled;
}
