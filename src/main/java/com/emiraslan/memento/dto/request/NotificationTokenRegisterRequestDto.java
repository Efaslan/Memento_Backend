package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTokenRegisterRequestDto {

    @NotBlank(message = "FCM_TOKEN_REQUIRED")
    @Size(max = 255, message = "FCM_TOKEN_TOO_LONG")
    private String fcmToken;

    @NotBlank(message = "REFRESH_TOKEN_REQUIRED_FOR_DEVICE_LINKING")
    private String refreshToken;
}