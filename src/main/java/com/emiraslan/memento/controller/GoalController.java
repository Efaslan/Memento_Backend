package com.emiraslan.memento.controller;

import com.emiraslan.memento.dto.request.GoalLogRequestDto;
import com.emiraslan.memento.dto.request.GoalRequestDto;
import com.emiraslan.memento.dto.response.ActiveGoalsResponseDto;
import com.emiraslan.memento.dto.response.GoalLogResponseDto;
import com.emiraslan.memento.dto.response.GoalResponseDto;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.GoalLogService;
import com.emiraslan.memento.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "09 - Goals")
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

    private final GoalService goalService;
    private final GoalLogService goalLogService;

    // --- PATIENT OPERATIONS ---
    @Operation(summary = "For patient users. Retrieves their own active goals.")
    @PreAuthorize("hasAuthority('PATIENT')")
    @GetMapping("/active/me")
    public ResponseEntity<List<ActiveGoalsResponseDto>> getMyActiveGoals(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(goalService.getActiveGoalsByPatient(user.getUserId()));
    }

    // --- RELATIVE OPERATIONS ---
    @Operation(
            description = "For relatives. Retrieves patient's active goals. Accessible only if you have an active relationship with the patient."
    )
    @PreAuthorize("hasAuthority('RELATIVE') and @guard.isThePatientOrTheirRelative(#patientId, principal)")
    @GetMapping("/active/patient/{patientId}")
    public ResponseEntity<List<ActiveGoalsResponseDto>> getPatientActiveGoals(@PathVariable Integer patientId) {
        return ResponseEntity.ok(goalService.getActiveGoalsByPatient(patientId));
    }

    // --- MUTUAL OPERATIONS (Create, Update, Delete) ---

    @Operation(
            summary = "For goal details page.",
            description = "Retrieves patient's goals (without today's logs) filtered by active/inactive status."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'PATIENT') and @guard.isThePatientOrTheirRelative(#patientId, principal)")
    @GetMapping("/patient/{patientId}/deactivated")
    public ResponseEntity<List<GoalResponseDto>> getDeactivatedGoals(@PathVariable Integer patientId) {
        return ResponseEntity.ok(goalService.getDeactivatedGoals(patientId));
    }

    @Operation(
            description = "For all users. patientUserId in DTO is optional for patients but required for relatives."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'PATIENT') and @guard.isThePatientOrTheirRelative(#dto.patientUserId, principal)")
    @PostMapping
    public ResponseEntity<GoalResponseDto> createGoal(
            @Valid @RequestBody GoalRequestDto dto,
            @AuthenticationPrincipal User creator
    ) {

        return ResponseEntity.ok(goalService.createGoal(dto, creator));
    }

    @PreAuthorize("hasAnyAuthority('RELATIVE', 'PATIENT') and @guard.canModifyGoal(#goalId, principal)")
    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponseDto> updateGoal(
            @PathVariable Integer goalId,
            @Valid @RequestBody GoalRequestDto dto
    ) {
        return ResponseEntity.ok(goalService.updateGoal(goalId, dto));
    }

    @PreAuthorize("hasAnyAuthority('RELATIVE', 'PATIENT') and @guard.canModifyGoal(#goalId, principal)")
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deactivateGoal(@PathVariable Integer goalId) {
        goalService.deactivateGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    // Goal Log Endpoints

    @Operation(
            summary = "For patients only. Upserts today's log for a specific goal.",
            description = "Creates a new log for today if it doesn't exist, or updates the existing one."
    )
    @PreAuthorize("hasAuthority('PATIENT') and @guard.isGoalOwner(#goalId, principal)")
    @PutMapping("/{goalId}/today")
    public ResponseEntity<GoalLogResponseDto> upsertTodayGoalLog(
            @PathVariable Integer goalId,
            @Valid @RequestBody GoalLogRequestDto dto
    ) {
        return ResponseEntity.ok(goalLogService.upsertTodayGoalLog(goalId, dto));
    }

    @Operation(
            summary = "Retrieves past logs of a specific goal.",
            description = "Fetches the logs within a specified date range (default is last 7 days)."
    )
    @PreAuthorize("hasAnyAuthority('RELATIVE', 'PATIENT') and @guard.canModifyGoal(#goalId, principal)")
    @GetMapping("/{goalId}/logs")
    public ResponseEntity<List<GoalLogResponseDto>> getLogsByGoal(
            @PathVariable Integer goalId,
            @RequestParam(required = true, defaultValue = "7") @Range(min = 7, max = 30) Integer daysBack
    ) {
        return ResponseEntity.ok(goalLogService.getLogsByGoal(goalId, daysBack));
    }
}