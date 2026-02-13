package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.RelationshipDto;
import com.emiraslan.memento.dto.RelationshipInitiationDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.RelationshipService;
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
@Tag(name = "04 - Relationships")
@SecurityRequirement(name = "bearerAuth")
public class RelationshipController {

    private final RelationshipService relationshipService;

    // lists all relationships of a patient
    @Operation(description = "All relationships of a user.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE')")
    @GetMapping("/me")
    public ResponseEntity<List<RelationshipDto>> getMyRelationships(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(relationshipService.getRelationships(user));
    }

    @Operation(summary = "Add someone to your family and create a relationship.",
            description = "Patients can add their relatives. Relatives cannot initiate relationships. Type can be: WIFE, HUSBAND, SON, DAUGHTER, OTHER."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isFamilyMember(#familyId, principal)")
    @PostMapping("/families/{familyId}/members")
    public ResponseEntity<RelationshipDto> addFamilyMemberAndRelationship(
            @PathVariable Integer familyId,
            @Valid @RequestBody RelationshipInitiationDto dto,
            @AuthenticationPrincipal User initiator) {
        return ResponseEntity.ok(relationshipService.addMemberAndRelationship(familyId, dto, initiator));
    }

    @Operation(description = "Updates relationship type and/or primary contact status. Patients cannot update relationships once they establish them.")
    @PreAuthorize("hasAuthority('RELATIVE') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @PutMapping("/{relationshipId}")
    public ResponseEntity<RelationshipDto> updateRelationship(
            @PathVariable Integer relationshipId,
            @Valid @RequestBody RelationshipDto dto
    ) {
        return ResponseEntity.ok(relationshipService.updateRelationship(relationshipId, dto));
    }

    @PreAuthorize("hasAnyAuthority('RELATIVE') and @guard.canUpdateRelationship(#relationshipId, principal)")
    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deleteRelationship(@PathVariable Integer relationshipId) {
        relationshipService.deleteRelationship(relationshipId);
        return ResponseEntity.noContent().build();
    }
}