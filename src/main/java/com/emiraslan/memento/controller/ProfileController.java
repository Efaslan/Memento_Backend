package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.DoctorProfileRequestDto;
import com.emiraslan.memento.dto.request.PatientProfileRequestDto;
import com.emiraslan.memento.dto.response.DoctorProfileResponseDto;
import com.emiraslan.memento.dto.response.PatientProfileResponseDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
            description = "Only patients and doctors have profiles."
    )
    @GetMapping("/me")
    public ResponseEntity<Object> getMyProfile(@AuthenticationPrincipal User user) {
        if (user.getRole() == UserRole.PATIENT) {
            return ResponseEntity.ok(profileService.getPatientProfile(user.getUserId()));
        } else if (user.getRole() == UserRole.DOCTOR) {
            return ResponseEntity.ok(profileService.getDoctorProfile(user.getUserId()));
        } else {
            // relatives do not have profiles
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            description = "You can only edit your own profile(patient). Id is automatically set."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PutMapping("/patient/me")
    public ResponseEntity<PatientProfileResponseDto> updateMyPatientProfile(
            @Valid @RequestBody PatientProfileRequestDto dto,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(profileService.updatePatientProfile(patient.getUserId(), dto));
    }

    @Operation(
            description = "You can only edit your own profile(doctor). Id is automatically set."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @PutMapping("/doctor/me")
    public ResponseEntity<DoctorProfileResponseDto> updateMyDoctorProfile(
            @Valid @RequestBody DoctorProfileRequestDto dto,
            @AuthenticationPrincipal User doctor
    ) {
        return ResponseEntity.ok(profileService.updateDoctorProfile(doctor.getUserId(), dto));
    }

    @Operation(
            summary = "View a patient's profile for Doctors and Relatives. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PatientProfileResponseDto> getPatientProfileById(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(profileService.getPatientProfile(patientId));
    }
}