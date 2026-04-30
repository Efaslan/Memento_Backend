package com.emiraslan.memento.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationLogSummaryResponseDto {
    private int takenCount;
    private int delayedCount;
    private int skippedCount;

    private Page<MedicationLogResponseDto> logs;

    public interface StatsProjection {
        int getTakenCount();
        int getDelayedCount();
        int getSkippedCount();
    }
}
