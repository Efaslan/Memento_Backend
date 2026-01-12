package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.GeneralReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "05 - General Reminders")
@SecurityRequirement(name = "bearerAuth")
public class GeneralReminderController {

    private final GeneralReminderService reminderService;

    // patient operations
    @Operation(summary = "For patient users.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/active/me")
    public ResponseEntity<List<GeneralReminderDto>> getMyActiveReminders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reminderService.getAllOngoingRemindersByPatient(user.getUserId()));
    }

    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/history/me")
    public ResponseEntity<List<GeneralReminderDto>> getMyCompletedReminders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reminderService.getCompletedRemindersByPatient(user.getUserId()));
    }

    // doctor / relative operations
    @Operation(
            description = "For doctors and relatives. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/active/patient/{patientId}")
    public ResponseEntity<List<GeneralReminderDto>> getPatientActiveReminders(@PathVariable Integer patientId) {
        return ResponseEntity.ok(reminderService.getAllOngoingRemindersByPatient(patientId));
    }

    @Operation(
            description = "For doctors and relatives. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/history/patient/{patientId}")
    public ResponseEntity<List<GeneralReminderDto>> getPatientCompletedReminders(@PathVariable Integer patientId) {
        return ResponseEntity.ok(reminderService.getCompletedRemindersByPatient(patientId));
    }

    // mutual operations (Create, Update, Delete)
    @Operation(
            description = "For all users. Id is automatically set if the user is a patient. Accessible for doctors and relatives only if they have an active relationship with the patient."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE', 'PATIENT') and @guard.canCreateReminder(#dto, principal)")
    @PostMapping
    public ResponseEntity<GeneralReminderDto> createReminder(@RequestBody GeneralReminderDto dto, @AuthenticationPrincipal User creator) {
        return ResponseEntity.ok(reminderService.createReminder(dto, creator));
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE', 'PATIENT') and @guard.canModifyReminder(#reminderId, principal)")
    @PutMapping("/{reminderId}")
    public ResponseEntity<GeneralReminderDto> updateReminder(
            @PathVariable Integer reminderId,
            @RequestBody GeneralReminderDto dto
    ) {
        return ResponseEntity.ok(reminderService.updateReminder(reminderId, dto));
    }

    @Operation(summary = "Set reminder as completed.")
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE', 'PATIENT') and @guard.canModifyReminder(#reminderId, principal)")
    @PatchMapping("/{reminderId}/complete")
    public ResponseEntity<GeneralReminderDto> markAsCompleted(@PathVariable Integer reminderId) {
        return ResponseEntity.ok(reminderService.markAsCompleted(reminderId));
    }

    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE', 'PATIENT') and @guard.canModifyReminder(#reminderId, principal)")
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Integer reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}