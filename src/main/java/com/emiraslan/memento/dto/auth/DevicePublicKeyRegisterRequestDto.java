package com.emiraslan.memento.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePublicKeyRegisterRequestDto {

    @NotBlank(message = "PUBLIC_KEY_REQUIRED")
    private String publicKey;

    @NotNull(message = "DEVICE_ID_REQUIRED")
    private Integer deviceId;

    @NotNull(message = "BIOMETRIC_ENABLED_NOT_BLANK")
    private Boolean biometricEnabled;
}
