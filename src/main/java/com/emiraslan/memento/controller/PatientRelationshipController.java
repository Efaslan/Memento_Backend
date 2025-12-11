package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.PatientRelationshipDto;
import com.emiraslan.memento.service.PatientRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "03 - Relationships")
public class PatientRelationshipController {

    private final PatientRelationshipService relationshipService;

    // lists all active relationships of a patient, excludeDoctors=true to filter doctors out
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PatientRelationshipDto>> getRelationships(
            @PathVariable Integer patientId,
            @RequestParam(defaultValue = "false") boolean excludeDoctors
    ) {
        return ResponseEntity.ok(relationshipService.getActiveRelationships(patientId, excludeDoctors));
    }

    // adds a new relationship with an email
    @PostMapping
    public ResponseEntity<PatientRelationshipDto> addRelationship(@RequestBody PatientRelationshipDto dto) {
        return ResponseEntity.ok(relationshipService.addRelationship(dto));
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deactivateRelationship(@PathVariable Integer relationshipId) {
        relationshipService.deactivateRelationship(relationshipId);
        return ResponseEntity.noContent().build();
    }

    // toggle to change primary contact status
    @Operation(
            summary = "Sets the primary contact status as true or false.",
            description = "Primary contacts receive notifications during alerts, such as when the Patient falls."
    )
    @PatchMapping("/{relationshipId}/toggle-primary")
    public ResponseEntity<PatientRelationshipDto> togglePrimaryStatus(@PathVariable Integer relationshipId) {
        return ResponseEntity.ok(relationshipService.togglePrimaryContactStatus(relationshipId));
    }
}