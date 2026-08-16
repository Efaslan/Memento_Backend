package com.emiraslan.memento.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogWithWaterResponseDto {
    private DailyLogResponseDto dailyLogDto;

    private WaterSummaryDto waterSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterSummaryDto {
        private Double progressValue;
        private Double logTargetValue;
        private String logUnit;
    }
}
