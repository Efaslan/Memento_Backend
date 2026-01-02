package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.PatientRelationshipDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.PatientRelationshipService;
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
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "03 - Relationships")
@SecurityRequirement(name = "bearerAuth")
public class PatientRelationshipController {

    private final PatientRelationshipService relationshipService;

    // lists all active relationships of a patient
    @Operation(description = "All relationships of a user. You can filter doctors out by setting the boolean as true.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE')")
    @GetMapping("/me")
    public ResponseEntity<List<PatientRelationshipDto>> getMyRelationships(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean excludeDoctors
    ) {
        return ResponseEntity.ok(relationshipService.getActiveRelationships(user, excludeDoctors));
    }

    @Operation(description = "Deactivated relationships of a user.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE')")
    @GetMapping("/me/inactive")
    public ResponseEntity<List<PatientRelationshipDto>> getMyInactiveRelationships(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(relationshipService.getInactiveRelationships(user));
    }

    @Operation(description = "Doctors can add patients, and patients can add their relatives through their emails (targetEmail). Relatives cannot initiate relationships.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR')")
    @PostMapping
    public ResponseEntity<PatientRelationshipDto> addRelationship(
            @Valid @RequestBody PatientRelationshipDto dto,
            @AuthenticationPrincipal User initiator) {
        return ResponseEntity.ok(relationshipService.addRelationship(dto, initiator));
    }

    @Operation(description = "Updates relationship type and/or primary contact status. Doctor relationships can only be edited by doctors.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PutMapping("/{relationshipId}")
    public ResponseEntity<PatientRelationshipDto> updateRelationship(
            @PathVariable Integer relationshipId,
            @Valid @RequestBody PatientRelationshipDto dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(relationshipService.updateRelationship(relationshipId, dto, user));
    }

    @Operation(description = "Activate or deactivate a relationship.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PatchMapping("/{relationshipId}/toggle-active")
    public ResponseEntity<PatientRelationshipDto> toggleActivation(@PathVariable Integer relationshipId) {
        return ResponseEntity.ok(relationshipService.toggleActivation(relationshipId));
    }

    @Operation(
            summary = "Sets the primary contact status as true or false.",
            description = "Primary contacts receive notifications during alerts, such as when the Patient falls."
    )
    @PreAuthorize("hasAnyAuthority('PATIENT') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PatchMapping("/{relationshipId}/toggle-primary")
    public ResponseEntity<PatientRelationshipDto> togglePrimaryStatus(@PathVariable Integer relationshipId) {
        return ResponseEntity.ok(relationshipService.togglePrimaryContactStatus(relationshipId));
    }
}