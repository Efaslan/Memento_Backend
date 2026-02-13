package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.PatientProfileDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "02 - Profiles")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    // DTOs contain both profile info and some more from the user table. This is so that the user can update their entire profile from a single form
    private final ProfileService profileService;

    @Operation(
            description = "Only patients have profiles."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<Object> getMyProfile(@AuthenticationPrincipal User user) {
            return ResponseEntity.ok(profileService.getPatientProfile(user.getUserId()));
    }

    @Operation(
            description = "You can only edit your own profile(patient). Id is automatically set."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PutMapping("/patient/me")
    public ResponseEntity<PatientProfileDto> updateMyPatientProfile(
            @RequestBody PatientProfileDto dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(profileService.updatePatientProfile(user.getUserId(), dto));
    }

    @Operation(
            summary = "View a patient's profile for Relatives. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAuthority('RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PatientProfileDto> getPatientProfileById(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(profileService.getPatientProfile(patientId));
    }
}