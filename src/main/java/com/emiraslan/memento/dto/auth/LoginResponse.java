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
    private String token; // JWT
    private UserResponseDto user;
}
