package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.AlertDto;
import com.emiraslan.memento.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // Mobile detects fall -> Creates PENDING alert
    @PostMapping("/fall")
    public ResponseEntity<AlertDto> createFallAlert(@RequestBody AlertDto dto) {
        return ResponseEntity.ok(alertService.createAlert(dto));
    }

    // User presses "I'm OK" within 30s -> Status becomes CANCELLED
    @PostMapping("/{alertId}/cancel")
    public ResponseEntity<AlertDto> cancelAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.cancelAlert(alertId));
    }

    // 30s passed without cancel -> mobile triggers this to send notifications -> Status becomes SENT
    @PostMapping("/{alertId}/send")
    public ResponseEntity<AlertDto> confirmAndSendAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.confirmFallAlert(alertId));
    }

    // Caregiver clicks "I'm handling it" -> Status becomes ACKNOWLEDGED
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable Integer alertId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId));
    }

    // History for patient dashboard
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AlertDto>> getPatientAlerts(@PathVariable Integer patientId) {
        return ResponseEntity.ok(alertService.getPatientAlerts(patientId));
    }
}