package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.DailyLogRequestDto;
import com.emiraslan.memento.dto.response.DailyLogResponseDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.DailyLogService;
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
@RequestMapping("/api/v1/dailylogs")
@RequiredArgsConstructor
@Tag(name = "06 - Daily Logs")
@SecurityRequirement(name = "bearerAuth")
public class DailyLogController {

    private final DailyLogService dailyLogService;

    // Patient endpoints
    @Operation(
            summary = "Last {days} daily logs. 0 returns today."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/my/recent/{days}")
    public ResponseEntity<List<DailyLogResponseDto>> getMyRecentLogs(
            @PathVariable Integer days,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(dailyLogService.getRecentLogs(user.getUserId(), days));
    }

    @Operation(
            description = "Patients can only create daily logs for themselves, id is automatically set. Type must be either FOOD or WATER."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping
    public ResponseEntity<DailyLogResponseDto> createLog(
            @Valid @RequestBody DailyLogRequestDto dto,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(dailyLogService.createLog(dto, patient));
    }

    @Operation(
            description = "Patients can update the description, type, and quantityMl of a daily log."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isDailyLogOwner(#logId, principal)")
    @PutMapping("/{logId}")
    public ResponseEntity<DailyLogResponseDto> updateLog(
            @PathVariable Integer logId,
            @Valid @RequestBody DailyLogRequestDto dto
    ) {
        return ResponseEntity.ok(dailyLogService.updateLog(logId, dto));
    }

    @DeleteMapping("/{logId}")
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isDailyLogOwner(#logId, principal)")
    public ResponseEntity<Void> deleteLog(@PathVariable Integer logId) {
        dailyLogService.deleteLog(logId);
        return ResponseEntity.noContent().build();
    }

    @Operation( // todo silinecek bu her gun icin 1 log oldugunda, zaten su dahil olucak yemeklerle beraber
            description = "The amount of water the patient has drunk today."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/my/water-total")
    public ResponseEntity<Integer> getMyTodayWaterTotal(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dailyLogService.getTodayTotalWaterIntake(user.getUserId()));
    }

    // doctor / relative endpoints

    @Operation(summary = "A patient's daily logs for doctors and relatives")
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/{patientId}/recent/{days}")
    public ResponseEntity<List<DailyLogResponseDto>> getPatientRecentLogs(
            @PathVariable Integer patientId,
            @PathVariable Integer days
    ) {
        return ResponseEntity.ok(dailyLogService.getRecentLogs(patientId, days));
    }
}