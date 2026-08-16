package com.emiraslan.memento.dto.response.medication;

import org.springframework.data.domain.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivatedSchedulesResponseDto {

    private Page<MedicationScheduleResponseDto> schedules;

    private MedicationStatsResponseDto medicationStats;
}
