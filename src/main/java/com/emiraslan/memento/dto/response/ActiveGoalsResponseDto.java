package com.emiraslan.memento.dto.response;

import com.emiraslan.memento.enums.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveGoalsResponseDto {
    private GoalResponseDto goalDto;

    private TodayGoalLogDto todayGoalLog;
    private Integer streak;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TodayGoalLogDto {
        private Integer goalLogId;
        private GoalStatus status;
        private Double progressValue;
        private Double logTargetValue;
        private String logUnit;
    }
}
