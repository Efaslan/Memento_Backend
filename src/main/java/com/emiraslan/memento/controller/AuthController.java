package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.auth.LoginRequest;
import com.emiraslan.memento.dto.auth.LoginResponse;
import com.emiraslan.memento.dto.auth.LogoutRequest;
import com.emiraslan.memento.dto.auth.RegisterRequest;
import com.emiraslan.memento.dto.response.UserResponseDto;
import com.emiraslan.memento.service.AuthService;
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        authService.logout(request.getFcmToken(), jwt);

        return ResponseEntity.ok().build();
    }
}
