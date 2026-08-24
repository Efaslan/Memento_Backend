package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.response.BasicStringResponse;
import com.emiraslan.memento.dto.response.RelationshipResponseDto;
import com.emiraslan.memento.dto.request.RelationshipRequestDto;
import com.emiraslan.memento.dto.auth.EmailDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.PatientRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "05 - Relationships")
@SecurityRequirement(name = "bearerAuth")
public class PatientRelationshipController {

    private final PatientRelationshipService relationshipService;

    // lists all active relationships of a patient
    @Operation(description = "All relationships of a user.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE')")
    @GetMapping("/me")
    public ResponseEntity<List<RelationshipResponseDto>> getMyRelationships(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(relationshipService.getActiveRelationships(user));
    }

    @Operation(
            summary = "Patients need to request OTP for relationship invitations.",
            description = "Sends a 6-digit OTP to the target email. Valid for 10 minutes."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping("/request")
    public ResponseEntity<BasicStringResponse> requestRelationship(
            @RequestBody @Valid EmailDto dto,
            @AuthenticationPrincipal User initiator
    ) {
        return ResponseEntity.ok(relationshipService.relationshipRequestByPatient(dto.getEmail(), initiator));
    }

    @Operation(description = "Patients can add their relatives if they provide the 6-digit OTP sent to their relative's email. Relatives cannot initiate relationships. Type can be: WIFE, HUSBAND, SON, DAUGHTER, OTHER.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping
    public ResponseEntity<RelationshipResponseDto> addRelationship(
            @Valid @RequestBody RelationshipRequestDto dto,
            @AuthenticationPrincipal User initiator) {
        return ResponseEntity.ok(relationshipService.addRelationship(dto, initiator));
    }

    @Operation(
            summary = "Sets the primary contact status as true or false.",
            description = "Primary contacts receive notifications during alerts, such as when the Patient falls."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.canModifyRelationship(#relationshipId, principal)")
    @PatchMapping("/{relationshipId}/toggle-primary")
    public ResponseEntity<RelationshipResponseDto> togglePrimaryStatus(@PathVariable Integer relationshipId) {
        return ResponseEntity.ok(relationshipService.togglePrimaryContactStatus(relationshipId));
    }

    @Operation(
            summary = "Deactivating relationships instead of deleting them."
    )
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.canModifyRelationship(#relationshipId, principal)")
    @PatchMapping("/{relationshipId}/deactivate")
    public ResponseEntity<Void> deactivateRelationship(@PathVariable Integer relationshipId) {
        relationshipService.deactivateRelationship(relationshipId);
        return ResponseEntity.noContent().build();
    }
}