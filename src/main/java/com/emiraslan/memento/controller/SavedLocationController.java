package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.SavedLocationRequestDto;
import com.emiraslan.memento.dto.response.SavedLocationResponseDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.SavedLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "04 - Saved Locations")
@SecurityRequirement(name = "bearerAuth")
public class SavedLocationController {

    private final SavedLocationService locationService;

     // brings all locations of a patient
    @Operation(description = "Only Patient users can have locations.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<List<SavedLocationResponseDto>> getMyLocations(@AuthenticationPrincipal User patient) {
        return ResponseEntity.ok(locationService.getLocationsByPatient(patient.getUserId()));
    }

    @Operation(description = "Only Patient users can create locations.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping
    public ResponseEntity<SavedLocationResponseDto> createLocation(
            @Valid @RequestBody SavedLocationRequestDto dto,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(locationService.createLocation(dto, patient));
    }

    @PreAuthorize("hasAuthority('PATIENT') and @guard.isLocationOwner(#locationId, principal)")
    @PutMapping("/{locationId}")
    public ResponseEntity<SavedLocationResponseDto> updateLocation(
            @PathVariable Integer locationId,
            @Valid @RequestBody SavedLocationRequestDto dto
    ) {
        return ResponseEntity.ok(locationService.updateLocation(locationId, dto));
    }

    @PreAuthorize("hasAuthority('PATIENT') and @guard.isLocationOwner(#locationId, principal)")
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Integer locationId) {
        locationService.deleteLocation(locationId);
        // returns 204 (No Content), if successful
        return ResponseEntity.noContent().build();
    }
}