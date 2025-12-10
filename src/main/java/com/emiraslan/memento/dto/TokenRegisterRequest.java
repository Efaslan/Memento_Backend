package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRegisterRequest {
    private Integer userId;
    private String fcmToken;
    private DeviceType deviceType; // ANDROID
}