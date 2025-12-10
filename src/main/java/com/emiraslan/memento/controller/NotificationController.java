package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.TokenRegisterRequest;
import com.emiraslan.memento.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final FcmService fcmService;

    // mobile will call this on login
    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(@RequestBody TokenRegisterRequest request) {
        fcmService.saveToken(request);
        return ResponseEntity.ok().build();
    }
}