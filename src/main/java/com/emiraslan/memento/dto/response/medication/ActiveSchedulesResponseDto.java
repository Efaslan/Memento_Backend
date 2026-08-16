package com.emiraslan.memento.dto.response.medication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSchedulesResponseDto {
    private List<MedicationScheduleResponseDto> activeScheduleDtos;
    private MedicationStatsResponseDto activeScheduleStats;
}
