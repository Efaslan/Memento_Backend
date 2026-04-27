package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.TokenRegisterRequestDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "10 - Notifications")
public class NotificationController {

    private final FcmService fcmService;

    @Operation(
            summary = "Register or update the device's FCM token.",
            description = "Push notifications are used for Alerts, Medication reminders, General Reminders, and Daily Logs. The aim is to motivate, while not bothering, the users into keeping track of their medication and daily consumptions."
    )
    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(
            @Valid @RequestBody TokenRegisterRequestDto request,
            @AuthenticationPrincipal User user
            ) {
        fcmService.saveToken(request, user);
        return ResponseEntity.ok().build();
    }
}