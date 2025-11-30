package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.GeneralReminderDto;
import com.emiraslan.memento.service.GeneralReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class GeneralReminderController {

    private final GeneralReminderService reminderService;

    // all active (current) reminders
    @GetMapping("/active/{patientId}")
    public ResponseEntity<List<GeneralReminderDto>> getActiveReminders(@PathVariable Integer patientId) {
        return ResponseEntity.ok(reminderService.getAllOngoingRemindersByPatient(patientId));
    }

    // all inactive (past) reminders
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

    @PatchMapping("/{reminderId}/toggle")
    public ResponseEntity<GeneralReminderDto> toggleCompletion(@PathVariable Integer reminderId) {
        return ResponseEntity.ok(reminderService.toggleCompletion(reminderId));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Integer reminderId) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }
}