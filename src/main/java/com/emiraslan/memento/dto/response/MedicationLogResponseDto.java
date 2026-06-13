package com.emiraslan.memento.dto.response;

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
public class MedicationLogResponseDto {
    private Integer medicationLogId;
    private Integer scheduleTimeId;
    private LocalDateTime takenAt;
    private MedicationStatus status;
}