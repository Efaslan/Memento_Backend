package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.FamilyDto;
import com.emiraslan.memento.dto.FamilyNameDto;
import com.emiraslan.memento.dto.UserDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.FamilyService;
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
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
@Tag(name = "03 - Family")
@SecurityRequirement(name = "bearerAuth")
public class FamilyController {

    private final FamilyService familyService;

    @Operation(summary = "Patients can create a family.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping("/create")
    public ResponseEntity<FamilyDto> createFamily(
            @Valid @RequestBody FamilyNameDto dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(familyService.createFamily(dto.getFamilyName(), user));
    }

    @Operation(summary = "Update family name.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.isFamilyMember(#familyId, principal)")
    @PutMapping("/{familyId}")
    public ResponseEntity<FamilyDto> updateFamilyName(
            @PathVariable Integer familyId,
            @Valid @RequestBody FamilyNameDto dto
    ) {
        return ResponseEntity.ok(familyService.updateFamilyName(familyId, dto.getFamilyName()));
    }

    @Operation(summary = "Remove a member from your family.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.isFamilyMember(#familyId, principal)")
    @DeleteMapping("/{familyId}/members/{userId}")
    public ResponseEntity<Void> removeFromFamily(
            @PathVariable Integer familyId,
            @PathVariable Integer userId
    ) {
        familyService.removeFromFamily(familyId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "My families", description = "Lists all families the user belongs to.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE')")
    @GetMapping("/me")
    public ResponseEntity<List<FamilyDto>> getMyFamilies(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(familyService.getUserFamilies(user.getUserId()));
    }

    @Operation(summary = "Family Members", description = "Lists all members of a specific family.")
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.isFamilyMember(#familyId, principal)")
    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<UserDto>> getFamilyMembers(@PathVariable Integer familyId) {
        return ResponseEntity.ok(familyService.getFamilyMembers(familyId));
    }
}
