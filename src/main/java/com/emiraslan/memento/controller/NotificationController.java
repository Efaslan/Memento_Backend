package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.TokenRegisterRequest;
import com.emiraslan.memento.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "09 - Notifications")
public class NotificationController {

    private final FcmService fcmService;

    @Operation(
            summary = "Register or update the device's FCM token.",
            description = "Push notifications are used for Alerts, Medication reminders, General Reminders, and Daily Logs. The aim is to motivate, while not bothering, the users into keeping track of their medication and daily consumptions."
    )
    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(@RequestBody TokenRegisterRequest request) {
        fcmService.saveToken(request);
        return ResponseEntity.ok().build();
    }
}