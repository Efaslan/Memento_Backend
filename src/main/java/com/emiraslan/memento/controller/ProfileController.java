package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.DoctorProfileDto;
import com.emiraslan.memento.dto.PatientProfileDto;
import com.emiraslan.memento.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    // DTOs contain both profile info and some more from the user table. This is so that the user can update their entire profile from a single form
    private final ProfileService profileService;

    // patient endpoints
    @GetMapping("/patient/{id}")
    public ResponseEntity<PatientProfileDto> getPatientProfile(@PathVariable Integer id) {
        return ResponseEntity.ok(profileService.getPatientProfile(id));
    }

    @PutMapping("/patient/{id}")
    public ResponseEntity<PatientProfileDto> updatePatientProfile(
            @PathVariable Integer id,
            @RequestBody PatientProfileDto dto
    ) {
        return ResponseEntity.ok(profileService.updatePatientProfile(id, dto));
    }

    // doctor endpoints
    @GetMapping("/doctor/{id}")
    public ResponseEntity<DoctorProfileDto> getDoctorProfile(@PathVariable Integer id) {
        return ResponseEntity.ok(profileService.getDoctorProfile(id));
    }

    @PutMapping("/doctor/{id}")
    public ResponseEntity<DoctorProfileDto> updateDoctorProfile(
            @PathVariable Integer id,
            @RequestBody DoctorProfileDto dto
    ) {
        return ResponseEntity.ok(profileService.updateDoctorProfile(id, dto));
    }
}