package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.auth.*;
import com.emiraslan.memento.dto.auth.TokenRefreshRequestDto;
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

    // Register and Login endpoints
    @Operation(
            description = "Role can be: PATIENT, DOCTOR, or RELATIVE."
    )
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Verifying user emails after registration."
    )
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);

        return ResponseEntity.ok("EMAIL_VERIFIED");
    }

    @Operation(
            description = "You will receive a JWT on successful login."
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    // Refresh and Access (JWT) Token endpoints
    @Operation(
            summary = "Refresh the Access JWT Token",
            description = "Send your valid Refresh Token to get a new 1-hour JWT without logging in again."
    )
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody TokenRefreshRequestDto request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.getRefreshToken()));
    }

    // we never send the challenge text to the deviceId's device. We send it to whoever made that request's console,
    // so it is not possible to bother other users with challenge requests
    // a user can only have 1 passkey challenge at the same time to protect from overwrite attacks
    @Operation(
            summary = "Request challenge (random text) for Refresh Token.",
            description = "After a Refresh Token expires, we generate a 3-minute TTL random text for the private key in mobile device to sign."
    )
    @GetMapping("/passkey/challenge/{deviceId}")
    public ResponseEntity<String> getPasskeyChallenge(@PathVariable Integer deviceId) {
        String challenge = authService.generatePasskeyChallenge(deviceId);
        return ResponseEntity.ok(challenge); // return the challenge as a string response
    }

    @Operation(
            summary = "Verify the signed challenge (random text) for a new Refresh Token.",
            description = "If the backend verifies the private key's signature, it returns a new Refresh Token and JWT."
    )
    @PostMapping("/passkey/verify")
    public ResponseEntity<LoginResponse> verifyPasskeyLogin(@Valid @RequestBody PasskeyVerifyRequestDto request) {
        LoginResponse response = authService.verifyPasskeyAndLogin(request);
        return ResponseEntity.ok(response);
    }

    // Password Reset Endpoints
    @Operation(
            summary = "Request 6-digit OTP.",
            description = "It will be sent to your email's inbox, and will be valid for 5 minutes."
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