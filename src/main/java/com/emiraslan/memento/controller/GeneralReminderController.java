package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.service.GeneralReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "05 - General Reminders")
public class GeneralReminderController {

    private final GeneralReminderService reminderService;

    // all active (current) reminders
    @GetMapping("/active/{patientId}")
    public ResponseEntity<List<GeneralReminderDto>> getActiveReminders(@PathVariable Integer patientId) {
        return ResponseEntity.ok(reminderService.getAllOngoingRemindersByPatient(patientId));
    }

    @Operation(
            summary = "List of completed reminders."
    )
    @GetMapping("/history/{patientId}")
    public ResponseEntity<List<GeneralReminderDto>> getCompletedReminders(@PathVariable Integer patientId) {
        return ResponseEntity.ok(reminderService.getCompletedRemindersByPatient(patientId));
    }

    @PostMapping
    public ResponseEntity<GeneralReminderDto> createReminder(@RequestBody GeneralReminderDto dto) {
        return ResponseEntity.ok(reminderService.createReminder(dto));
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<GeneralReminderDto> updateReminder(
            @PathVariable Integer reminderId,
            @RequestBody GeneralReminderDto dto
    ) {
        return ResponseEntity.ok(reminderService.updateReminder(reminderId, dto));
    }

    @Operation(
            summary = "Set reminder as completed."
    )
    @PatchMapping("/{reminderId}/complete")
    public ResponseEntity<GeneralReminderDto> markAsCompleted(@PathVariable Integer reminderId) {
        return ResponseEntity.ok(reminderService.markAsCompleted(reminderId));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Integer reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}