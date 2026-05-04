package com.emiraslan.memento.dto.auth;

import com.emiraslan.memento.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UserResponseDto user;
    private String accessJwtToken;  // 1 hour jwt
    private String refreshToken; // 90 days refresh token

}
