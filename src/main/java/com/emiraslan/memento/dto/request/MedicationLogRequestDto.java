package com.emiraslan.memento.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationLogRequestDto {

    @NotNull(message = "SCHEDULE_TIME_ID_REQUIRED")
    private Integer scheduleTimeId;

    // "patientUserId" is taken from jwt
    // "takenAt" = now()
    // status is handled by service
}