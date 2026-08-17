package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.MedicationScheduleRequestDto;
import com.emiraslan.memento.dto.response.medication.ActiveSchedulesResponseDto;
import com.emiraslan.memento.dto.response.medication.DeactivatedSchedulesResponseDto;
import com.emiraslan.memento.dto.response.medication.MedicationScheduleResponseDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.medication.MedicationScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/medications/schedules")
@RequiredArgsConstructor
@Tag(name = "11 - Medication Schedules")
@SecurityRequirement(name = "bearerAuth")
public class MedicationScheduleController {

    private final MedicationScheduleService scheduleService;

    // -----------------PATIENT OPERATIONS-------------------

    @Operation(
            summary = "Active medications of the patient."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<ActiveSchedulesResponseDto> getMyActiveSchedules(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean includeStatistics
    ) {
        return ResponseEntity.ok(scheduleService.getActiveSchedules(user.getUserId(), includeStatistics));
    }

    @Operation(
            summary = "Entire medical history of the patient."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me/history")
    public ResponseEntity<DeactivatedSchedulesResponseDto> getMyScheduleHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Range(min = 1, max = 20) Integer size

            ) {
        return ResponseEntity.ok(scheduleService.getAllPastSchedulesByPatient(user.getUserId(), page, size));
    }

    // -----------------MUTUAL OPERATIONS-------------------

    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.canCreateSchedule(#dto, principal)")
    @PostMapping
    public ResponseEntity<MedicationScheduleResponseDto> createSchedule(
            @Valid @RequestBody MedicationScheduleRequestDto dto,
            @AuthenticationPrincipal User creator
    ) {
        // we force the creator's id from jwt instead of taking it from the dto
        return ResponseEntity.ok(scheduleService.createSchedule(dto, creator));
    }

    @Operation(
            summary = "Update an existing schedule. Please see descriptions.",
            description = "Updating a schedule is heavily restricted in order to protect a patient's medical history. Schedules can only be updated if the patient has no consumption logs on that schedule. If there are logs, only non-critical fields such as: Notes, End Date, and deactivation can be changed."
    )
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.canModifySchedule(#scheduleId, principal)")
    @PutMapping("/{scheduleId}")
    public ResponseEntity<MedicationScheduleResponseDto> updateSchedule(
            @PathVariable Integer scheduleId,
            @Valid @RequestBody MedicationScheduleRequestDto dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, dto));
    }

    @Operation(
            summary = "Deactivating schedules instead of deleting.",
            description = "This is in order to protect medical history. Patient's will not receive notifications about their deactivated schedules."
    )
    @PreAuthorize("hasAnyAuthority('PATIENT', 'RELATIVE') and @guard.canModifySchedule(#scheduleId, principal)")
    @PatchMapping("/{scheduleId}/deactivate")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // -----------------RELATIVE OPERATIONS-------------------

    @Operation(
            summary = "A patient's active schedules for a relative."
    )
    @PreAuthorize("hasAuthority('RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ActiveSchedulesResponseDto> getPatientActiveSchedules(
            @PathVariable Integer patientId,
            @RequestParam(defaultValue = "false") boolean includeStatistics
    ) {
        return ResponseEntity.ok(scheduleService.getActiveSchedules(patientId, includeStatistics));
    }

    @Operation(
            summary = "A patient's entire medical history for a relative."
    )
    @PreAuthorize("hasAuthority('RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<DeactivatedSchedulesResponseDto> getPatientScheduleHistory(
            @PathVariable Integer patientId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Range(min = 1, max = 20) Integer size
    ) {
        return ResponseEntity.ok(scheduleService.getAllPastSchedulesByPatient(patientId, page, size));
    }
}