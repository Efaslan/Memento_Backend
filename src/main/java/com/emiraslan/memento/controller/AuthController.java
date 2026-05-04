package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.auth.*;
import com.emiraslan.memento.dto.request.TokenRefreshRequestDto;
import com.emiraslan.memento.dto.response.UserResponseDto;
import com.emiraslan.memento.service.auth.AuthService;
import com.emiraslan.memento.service.auth.ResetPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "01 - Authentication")
public class AuthController {

    private final AuthService authService;
    private final ResetPasswordService resetPasswordService;

    @Operation(
            description = "Password must be 6 characters at least. Role can be: PATIENT, DOCTOR, or RELATIVE."
    )
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            description = "You will receive a JWT on successful login."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Refresh the Access JWT Token",
            description = "Send your valid Refresh Token to get a new 1-hour JWT without logging in again."
    )
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody TokenRefreshRequestDto request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader // header might not be present if the token is already expired
    ) {
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        authService.logout(request.getRefreshToken(), jwt);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Request 6-digit OTP.",
            description = "It will be sent to your email's inbox, and will be valid for 15 minutes."
    )
    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid EmailDto dto){
        resetPasswordService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok("We have sent you a 6-digit code you can use to reset your password. Please check your inbox.");
    }

    @Operation(
            summary = "Reset your password using the OTP in your inbox."
    )
    @PostMapping("/password-reset/reset")
    public ResponseEntity<String> resetPassword(
            @RequestBody @Valid ResetPasswordDto dto
    ){
        resetPasswordService.resetPassword(dto.getEmail(), dto.getOtpCode(), dto.getNewPassword());
        return ResponseEntity.ok("Your password has been successfully updated.");
    }
}
