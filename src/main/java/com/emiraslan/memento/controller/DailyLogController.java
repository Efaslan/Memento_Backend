package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.DailyLogDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.service.DailyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dailylogs")
@RequiredArgsConstructor
@Tag(name = "06 - Daily Logs")
@SecurityRequirement(name = "bearerAuth")
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @Operation(
            description = "Returns today's daily logs if no date is given."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping
    public ResponseEntity<List<DailyLogDto>> getMyLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(dailyLogService.getLogsByDate(user.getUserId(), targetDate));
    }

    @Operation(
            summary = "Last {days} daily logs."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/recent/{days}")
    public ResponseEntity<List<DailyLogDto>> getMyRecentLogs(
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
    public ResponseEntity<DailyLogDto> createLog(
            @RequestBody DailyLogDto dto,
            @AuthenticationPrincipal User user
    ) {
        dto.setPatientUserId(user.getUserId());

        return ResponseEntity.ok(dailyLogService.createLog(dto));
    }

    @DeleteMapping("/{logId}")
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isDailyLogOwner(#logId, principal)")
    public ResponseEntity<Void> deleteLog(@PathVariable Integer logId) {
        dailyLogService.deleteLog(logId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            description = "The amount of water the patient has drunk today."
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/my/water-total")
    public ResponseEntity<Integer> getMyTodayWaterTotal(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dailyLogService.getTodayTotalWaterIntake(user.getUserId()));
    }
}