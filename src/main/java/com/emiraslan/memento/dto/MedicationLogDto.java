package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.MedicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationLogDto {
    private Integer medicationLogId;
    private Integer scheduleTimeId;
    private Integer patientUserId;
    private LocalDateTime takenAt;
    private MedicationStatus status;
    private String medicationName; // for display
}