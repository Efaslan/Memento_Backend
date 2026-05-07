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
public class PasskeyVerifyRequestDto {

    // we return the device id to mobile in login response. The device always knows its id, and we use that id to find the device's public key.
    @NotNull(message = "DEVICE_ID_REQUIRED")
    private Integer deviceId;

    @NotBlank(message = "SIGNATURE_REQUIRED")
    private String signature; // random string signed by the private key in mobile device
}
