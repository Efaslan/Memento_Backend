package com.emiraslan.memento.dto.response.medication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationStatsResponseDto {
    private Integer takenPercentage;
    private Integer delayedPercentage;
    private Integer skippedPercentage;
    private Long totalPastLogs;
}
