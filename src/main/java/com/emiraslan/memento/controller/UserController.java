package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.auth.EmailDto;
import com.emiraslan.memento.dto.request.EmailChangeRequestDto;
import com.emiraslan.memento.dto.request.UserConsentRequest;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.UserService;
import com.emiraslan.memento.util.HttpRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "02 - User")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Request OTP for email change."
    )
    @PostMapping("/email/change-request")
    public ResponseEntity<String> requestEmailChange(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid EmailDto dto
    ) {
        userService.requestEmailChange(user.getUserId(), dto.getEmail());

        return ResponseEntity.ok("OTP_SENT_TO_NEW_EMAIL");
    }

    @Operation(
            summary = "Verify OTP and update email."
    )
    @PostMapping("/email/change-verify")
    public ResponseEntity<String> verifyEmailChange(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid EmailChangeRequestDto dto
    ) {
        userService.verifyAndChangeEmail(user.getUserId(), dto.getNewEmail(), dto.getOtpCode());

        return ResponseEntity.ok("EMAIL_SUCCESSFULLY_UPDATED");
    }

    @Operation(
            summary = "Record a user's consent to an agreement other than the Privacy Policy consented during registration."
    )
    @PostMapping("/consents")
    public ResponseEntity<String> submitConsent(
            @Valid @RequestBody UserConsentRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpServletRequest
    ) {
        String ipAddress = HttpRequestUtil.extractClientIp(httpServletRequest);
        String userAgent = HttpRequestUtil.extractUserAgent(httpServletRequest);

        userService.recordConsent(user, request,ipAddress, userAgent);

        return ResponseEntity.ok("CONSENT_RECORDED_SUCCESSFULLY");
    }
}