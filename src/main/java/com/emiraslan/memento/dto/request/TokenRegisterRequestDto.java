package com.emiraslan.memento.dto.request;

import com.emiraslan.memento.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRegisterRequestDto {

    @NotBlank(message = "FCM_TOKEN_REQUIRED")
    @Size(max = 255, message = "FCM_TOKEN_TOO_LONG")
    private String fcmToken;

    @NotNull(message = "DEVICE_TYPE_REQUIRED") // ANDROID
    private DeviceType deviceType;
}