package com.emiraslan.memento.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceResponseDto {

    private Integer deviceId;
    private String deviceModel;
    private String osVersion;
    private boolean biometricEnabled;
    private LocalDateTime lastActive;
}
