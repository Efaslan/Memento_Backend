package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.PatientCardDto;
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
@Tag(name = "03 - Relationships")
@SecurityRequirement(name = "bearerAuth")
public class PatientRelationshipController {

    private final PatientRelationshipService relationshipService;

    // lists all active relationships of a patient
    @Operation(description = "All relationships of a user.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE')")
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
    public ResponseEntity<String> requestRelationship(
            @RequestBody @Valid EmailDto dto,
            @AuthenticationPrincipal User initiator
    ) {
        relationshipService.relationshipRequestByPatient(dto.getEmail(), initiator);
        return ResponseEntity.ok("6-digit OTP successfully sent to the target email.");
    }

    @Operation(description = "Doctors can add patients directly. Patients can add anyone except doctors if they provide the 6-digit OTP sent to their relative's email. Relatives cannot initiate relationships. Type can be: DOCTOR, WIFE, HUSBAND, SON, DAUGHTER, OTHER.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR')")
    @PostMapping
    public ResponseEntity<RelationshipResponseDto> addRelationship(
            @Valid @RequestBody RelationshipRequestDto dto,
            @AuthenticationPrincipal User initiator) {
        return ResponseEntity.ok(relationshipService.addRelationship(dto, initiator));
    }

    @Operation(description = "Updates relationship type and/or primary contact status. Doctor relationships can only be edited by doctors.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'DOCTOR', 'RELATIVE') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PutMapping("/{relationshipId}")
    public ResponseEntity<RelationshipResponseDto> updateRelationship(
            @PathVariable Integer relationshipId,
            @Valid @RequestBody RelationshipResponseDto dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(relationshipService.updateRelationship(relationshipId, dto, user));
    }

    @Operation(
            summary = "Sets the primary contact status as true or false.",
            description = "Primary contacts receive notifications during alerts, such as when the Patient falls."
    )
    @PreAuthorize("hasAnyAuthority('PATIENT') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PatchMapping("/{relationshipId}/toggle-primary")
    public ResponseEntity<RelationshipResponseDto> togglePrimaryStatus(@PathVariable Integer relationshipId) {
        return ResponseEntity.ok(relationshipService.togglePrimaryContactStatus(relationshipId));
    }

    @Operation(summary = "Get paginated and searchable list of patients for a doctor")
    @PreAuthorize("hasAuthority('DOCTOR')")
    @GetMapping("/my-patients")
    public ResponseEntity<Slice<PatientCardDto>> getMyPatients(
            @AuthenticationPrincipal User doctor,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("patient.firstName").ascending());

        return ResponseEntity.ok(relationshipService.getDoctorPatients(doctor, search, pageable));
    }
}