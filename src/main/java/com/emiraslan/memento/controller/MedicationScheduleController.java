package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.MedicationScheduleDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.MedicationScheduleService;
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
@RequestMapping("/api/v1/medications/schedules")
@RequiredArgsConstructor
@Tag(name = "08 - Medication Schedules")
@SecurityRequirement(name = "bearerAuth")
public class MedicationScheduleController {

    private final MedicationScheduleService scheduleService;

    // -----------------PATIENT OPERATIONS-------------------

    @Operation(
            summary = "Active medications of the patient."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me")
    public ResponseEntity<List<MedicationScheduleDto>> getMyActiveSchedules(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(user.getUserId()));
    }

    @Operation(
            summary = "Entire medical history of the patient."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me/history")
    public ResponseEntity<List<MedicationScheduleDto>> getMyScheduleHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesByPatient(user.getUserId()));
    }

    @Operation(
            summary = "Medications a patient takes on need without a specific time."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/me/prn")
    public ResponseEntity<List<MedicationScheduleDto>> getMyPrnSchedules(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(scheduleService.getPrnSchedulesByPatient(user.getUserId()));
    }

    // -----------------DOCTOR OPERATIONS-------------------
    @Operation(
            summary = "Medications you(a doctor) prescribed."
    )
    @PreAuthorize("hasAuthority('DOCTOR')")
    @GetMapping("/doctor/me")
    public ResponseEntity<List<MedicationScheduleDto>> getMyPrescriptions(@AuthenticationPrincipal User doctorUser) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorUser.getUserId()));
    }

    @PreAuthorize("hasAuthority('DOCTOR') and @guard.canCreateSchedule(#dto, principal)")
    @PostMapping
    public ResponseEntity<MedicationScheduleDto> createSchedule(
            @RequestBody MedicationScheduleDto dto,
            @AuthenticationPrincipal User doctorUser
    ) {
        // we force the doctor's id from jwt instead of taking it from the dto
        dto.setDoctorUserId(doctorUser.getUserId());
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @Operation(
            summary = "Update an existing schedule. Please see descriptions.",
            description = "Updating a schedule is heavily restricted in order to protect a patient's medical history. Schedules can only be updated if the patient has no consumption logs on that schedule. If there are logs, only non-critical fields such as: Notes, End Date, and deactivation can be changed."
    )
    @PreAuthorize("hasAuthority('DOCTOR') and @guard.canModifySchedule(#scheduleId, principal)")
    @PutMapping("/{scheduleId}")
    public ResponseEntity<MedicationScheduleDto> updateSchedule(
            @PathVariable Integer scheduleId,
            @RequestBody MedicationScheduleDto dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(scheduleId, dto));
    }

    @Operation(
            summary = "Deactivating schedules instead of deleting.",
            description = "This is in order to protect medical history. Patient's will not receive notifications about their deactivated schedules."
    )
    @PreAuthorize("hasAuthority('DOCTOR') and @guard.canModifySchedule(#scheduleId, principal)")
    @PatchMapping("/{scheduleId}/deactivate")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Integer scheduleId) {
        scheduleService.deactivateSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }

    // -----------------DOCTOR/RELATIVE OPERATIONS-------------------

    @Operation(
            summary = "A patient's active schedules for a doctor or a relative."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicationScheduleDto>> getPatientActiveSchedules(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(scheduleService.getActiveSchedulesByPatient(patientId));
    }

    @Operation(
            summary = "A patient's entire medical history for a doctor or a relative."
    )
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<MedicationScheduleDto>> getPatientScheduleHistory(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(scheduleService.getAllSchedulesByPatient(patientId));
    }
}