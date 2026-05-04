package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.DailyLogRequestDto;
import com.emiraslan.memento.dto.response.DailyLogResponseDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.DailyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dailylogs")
@RequiredArgsConstructor
@Tag(name = "06 - Daily Logs")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class DailyLogController {

    private final DailyLogService dailyLogService;

    // Patient endpoints
    @Operation(
            summary = "Last {days} daily logs. 0 returns today."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/my/recent/{days}")
    public ResponseEntity<List<DailyLogResponseDto>> getMyRecentLogs(
            @PathVariable
            @Range(min = 0, max = 90, message = "Days back must be between 0 and 90.")
            Integer days,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(dailyLogService.getRecentLogs(user.getUserId(), days));
    }

    @Operation(
            summary = "Upsert Today's Log",
            description = "Creates or updates the daily log for the authenticated patient for TODAY. No ID required."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @PostMapping("/today")
    public ResponseEntity<DailyLogResponseDto> upsertTodayLog(
            @Valid @RequestBody DailyLogRequestDto dto,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(dailyLogService.upsertTodayLog(dto, patient));
    }

    @DeleteMapping("/{logId}")
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isDailyLogOwner(#logId, principal)")
    public ResponseEntity<Void> deleteLog(@PathVariable Integer logId) {
        dailyLogService.deleteLog(logId);
        return ResponseEntity.noContent().build();
    }

    // doctor / relative endpoints

    @Operation(summary = "A patient's daily logs for doctors and relatives")
    @PreAuthorize("hasAnyAuthority('DOCTOR', 'RELATIVE') and @guard.canViewPatientData(#patientId, principal)")
    @GetMapping("/{patientId}/recent/{days}")
    public ResponseEntity<List<DailyLogResponseDto>> getPatientRecentLogs(
            @PathVariable Integer patientId,
            @PathVariable
            @Range(min = 0, max = 90, message = "Days back must be between 0 and 90.")
            Integer days
    ) {
        return ResponseEntity.ok(dailyLogService.getRecentLogs(patientId, days));
    }
}