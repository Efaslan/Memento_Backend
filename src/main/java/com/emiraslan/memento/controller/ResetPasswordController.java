package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.auth.EmailDto;
import com.emiraslan.memento.dto.auth.ResetPasswordDto;
import com.emiraslan.memento.service.ResetPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
@Tag(name = "01 - Authentication")
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @Operation(
            summary = "Request 6-digit OTP.",
            description = "It will be sent to your email's inbox, and will be valid for 15 minutes."
    )
    @PostMapping("/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid EmailDto dto){
        resetPasswordService.requestPasswordReset(dto.getEmail());
        return ResponseEntity.ok("We have sent you a 6-digit code you can use to reset your password. Please check your inbox.");
    }

    @Operation(
            summary = "Reset your password using the OTP in your inbox."
    )
    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(
            @RequestBody @Valid ResetPasswordDto dto
            ){
        resetPasswordService.resetPassword(dto.getEmail(), dto.getOtpCode(), dto.getNewPassword());
        return ResponseEntity.ok("Your password has been successfully updated.");
    }
}
