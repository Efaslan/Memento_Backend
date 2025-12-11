package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.DailyLogDto;
import com.emiraslan.memento.service.DailyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dailylogs")
@RequiredArgsConstructor
@Tag(name = "06 - Daily Logs")
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @Operation(
            description = "This endpoint returns today's daily logs if no date is given."
    )
    @GetMapping
    public ResponseEntity<List<DailyLogDto>> getLogs(
            @RequestParam Integer patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(dailyLogService.getLogsByDate(patientId, targetDate));
    }

    @Operation(
            summary = "Last {days} daily logs."
    )
    @GetMapping("/recent/{days}")
    public ResponseEntity<List<DailyLogDto>> getRecentLogs(
            @PathVariable Integer days,
            @RequestParam Integer patientId
    ) {
        return ResponseEntity.ok(dailyLogService.getRecentLogs(patientId, days));
    }

    @PostMapping
    public ResponseEntity<DailyLogDto> createLog(@RequestBody DailyLogDto dto) {
        return ResponseEntity.ok(dailyLogService.createLog(dto));
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteLog(@PathVariable Integer logId) {
        dailyLogService.deleteLog(logId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            description = "The amount of water the patient has drunk today."
    )
    @GetMapping("/water-total/{patientId}")
    public ResponseEntity<Integer> getTodayWaterTotal(@PathVariable Integer patientId) {
        return ResponseEntity.ok(dailyLogService.getTodayTotalWaterIntake(patientId));
    }
}