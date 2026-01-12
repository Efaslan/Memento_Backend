package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.AlertDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.service.AlertService;
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
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "07 - Alerts")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @Operation(
            summary = "Called when a patient falls.",
            description = "Creates a pending status alert log when the mobile phone falls down. PatientId is automatically set."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping("/fall")
    public ResponseEntity<AlertDto> createFallAlert( // patients create alerts for themselves
            @Valid @RequestBody AlertDto dto,
            @AuthenticationPrincipal User user
    ) {
        dto.setPatientUserId(user.getUserId()); // manually assign id to the dto
        dto.setStatus(AlertStatus.PENDING);
        return ResponseEntity.ok(alertService.createAlert(dto));
    }

    @Operation(
            summary = "Cancel false alerts.",
            description = "If the user presses `I'm OK.` within 30 seconds, the alert is cancelled."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isAlertOwner(#alertId, principal)")
    @PostMapping("/{alertId}/cancel")
    public ResponseEntity<AlertDto> cancelAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.cancelAlert(alertId));
    }

    @Operation(
            summary = "Send notifications about the alert.",
            description = "If the user remains unresponsive for 30 seconds, notifications are sent to primary contacts."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isAlertOwner(#alertId, principal)")
    @PostMapping("/{alertId}/send")
    public ResponseEntity<AlertDto> confirmAndSendAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.confirmFallAlert(alertId));
    }

    @Operation(
            summary = "A relative is checking the situation.",
            description = "Alert status becomes 'acknowledged' when a primary contact affirms the notification."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'DOCTOR') and @guard.canAcknowledgeAlert(#alertId, principal)")
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(
            @PathVariable Integer alertId,
            @AuthenticationPrincipal User caregiver
    ) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId, caregiver.getUserId()));
    }

    @Operation(
            description = "A patient's history of critical situations. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'DOCTOR') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AlertDto>> getPatientAlerts(
            @PathVariable Integer patientId
    ) {
        return ResponseEntity.ok(alertService.getPatientAlerts(patientId));
    }
}