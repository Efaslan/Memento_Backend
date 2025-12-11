package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.AlertDto;
import com.emiraslan.memento.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "07 - Alerts")
public class AlertController {

    private final AlertService alertService;

    @Operation(
            summary = "Called when a patient falls.",
            description = "Creates a pending status alert log when the mobile phone falls down."
    )
    @PostMapping("/fall")
    public ResponseEntity<AlertDto> createFallAlert(@RequestBody AlertDto dto) {
        return ResponseEntity.ok(alertService.createAlert(dto));
    }

    @Operation(
            summary = "Cancel false alerts.",
            description = "If the user presses `I'm OK.` within 30 seconds, the alert is cancelled."
    )
    @PostMapping("/{alertId}/cancel")
    public ResponseEntity<AlertDto> cancelAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.cancelAlert(alertId));
    }

    @Operation(
            summary = "Send notifications about the alert.",
            description = "If the user remains unresponsive for 30 seconds, notifications are sent to primary contacts."
    )
    @PostMapping("/{alertId}/send")
    public ResponseEntity<AlertDto> confirmAndSendAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.confirmFallAlert(alertId));
    }

    @Operation(
            summary = "A relative is checking the situation.",
            description = "Alert status becomes 'acknowledged' when a primary contact affirms the notification."
    )
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable Integer alertId, @RequestParam Integer caregiverId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId, caregiverId));
    }

    // History for patient dashboard
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AlertDto>> getPatientAlerts(@PathVariable Integer patientId) {
        return ResponseEntity.ok(alertService.getPatientAlerts(patientId));
    }
}